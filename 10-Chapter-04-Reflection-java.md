# Spring AI  实现：反思循环模式

## 1. 定义核心反思服务
``` java

@Service
public class ReflectionService {

    private final ChatClient chatClient;

    public ReflectionService(ChatClient.Builder builder) {
        // 我们不在这里绑定默认记忆，因为我们需要手动管理生成者和反思者的上下文
        this.chatClient = builder.build();
    }

    public String runReflectionLoop(String taskDescription, int maxIterations) {
        String currentCode = "";
        String lastCritique = "";
        
        // 使用 List 模拟 Python 中的 message_history
        List<Message> history = new ArrayList<>();
        history.add(new UserMessage(taskDescription));

        for (int i = 0; i < maxIterations; i++) {
            System.out.printf("\n========== 反思循环：第 %d 次迭代 ==========\n", i + 1);

            // --- 阶段 1：生成/优化 ---
            String promptText = (i == 0) ? taskDescription : "请根据批判意见优化代码：\n" + lastCritique;
            
            ChatResponse response = chatClient.prompt()
                    .messages(history) // 注入之前的上下文
                    .user(promptText)
                    .call()
                    .chatResponse();

            currentCode = response.getResult().getOutput().getContent();
            history.add(response.getResult().getOutput()); // 记录模型生成的代码
            
            System.out.println(">>> 阶段 1：生成代码完成");

            // --- 阶段 2：反思 ---
            System.out.println(">>> 阶段 2：进行代码审查...");
            lastCritique = chatClient.prompt()
                    .system("""
                        你是一名资深软件工程师，精通 Java/Python。
                        请根据任务要求严格评估代码，检查 Bug、风格和边界情况。
                        若代码完美，仅回复 'CODE_IS_PERFECT'。
                        否则，请列出具体的改进建议。
                        """)
                    .user("原始任务：\n" + taskDescription + "\n\n待审查代码：\n" + currentCode)
                    .call()
                    .content();

            if (lastCritique.contains("CODE_IS_PERFECT")) {
                System.out.println("--- 结果：代码已达完美，停止迭代 ---");
                break;
            }

            System.out.println("--- 批判意见 ---\n" + lastCritique);
            history.add(new UserMessage("上次代码的批判意见：\n" + lastCritique));
        }

        return currentCode;
    }
}
```

## 2. 如何在 Spring Boot 中运行

``` java
@SpringBootApplication
public class ReflectionApp implements CommandLineRunner {

    @Autowired
    private ReflectionService reflectionService;

    public static void main(String[] args) {
        SpringApplication.run(ReflectionApp.class, args);
    }

    @Override
    public void run(String... args) {
        String task = """
            创建一个 calculate_factorial 函数：
            1. 计算 n!
            2. 处理边界情况 (n=0)
            3. 负数抛出 ValueError
            """;
            
        String finalCode = reflectionService.runReflectionLoop(task, 3);
        System.out.println("\n最终优化代码：\n" + finalCode);
    }
}

```


为什么选择这种模式？
自纠错能力：单次 Prompt 往往难以涵盖所有边界条件。通过这种模式，AI 相当于在提交作业前自己检查了三遍。

降低幻觉：反思阶段强制模型以“审查者”视角观察输出，能有效发现第一遍生成时忽略的逻辑漏洞。

Spring 的优势：在 Java 环境下，你可以很容易地在“反思阶段”加入真实的 Unit Test（单元测试） 结果。如果测试不通过，将报错信息反馈给 AI，这比纯文本反思更具杀伤力。



# Spring AI 实现：顺序审查流水线

``` java

import com.strategist.ai.patterns.reflection.dto.ReviewResult;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class ReviewPipelineService {

    @Resource
    private final ChatClient chatClient;

    public ReviewPipelineService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public ReviewResult runWriteAndReview(String topic) {
        // --- 1. 执行 DraftWriter (生成初稿) ---
        String draftText = chatClient.prompt()
                .system("你是一个专业的初稿撰写员，请写一段简短、信息丰富的主题段落。")
                .user(topic)
                .call()
                .content();

        System.out.println(">>> 初稿已生成: " + draftText);

        // --- 2. 执行 FactChecker (事实核查并结构化输出) ---
        // 我们直接将上一步的 draftText 传递给审查员
        return chatClient.prompt()
                .system("你是一名严谨的事实核查员。请核查用户提供的文本。")
                .user("待核查文本：\n" + draftText)
                .call()
                .entity(ReviewResult.class); // 核心：自动转换为 Java 对象
    }
}

```
``` java
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Data;

@Data
public class ReviewResult {

    @JsonPropertyDescription("状态：ACCURATE 或 INACCURATE")
    String status;

    @JsonPropertyDescription("核查的详细逻辑和改进建议")
    String reasoning;

}

```

## 为什么在 Spring 环境下更稳健？
强类型解析：在 Python 中，review_output 只是一个普通的字典，访问 result["status"] 如果拼错会报错。而在 Java 中，reviewResult.status() 是编译时确定的。

上下文隔离：你可以为 DraftWriter 和 FactChecker 配置不同的模型。例如：生成初稿用较便宜的模型（如 DeepSeek-V3），而事实核查使用更严谨、逻辑更强的模型（如 GPT-4o 或 Qwen-Max）。

易于监控：你可以轻松地在两个步骤之间插入 Spring 的监控埋点，记录每个阶段的 Token 消耗和耗时。

进阶：如何增加“自我修正”？

目前的流水线只到审查为止。如果要实现真正的闭环，可以增加一个判断逻辑：如果 status 是 INACCURATE，则把 reasoning 重新发给 DraftWriter 要求重写。


# 核心实现：带反馈的自动化流水线

## 1. 完善结构化对象
首先，确保审查员能给出清晰的反馈，让作者知道哪里错了。
``` java
public record ReviewResult(
    String status,    // "APPROVED" 或 "REJECTED"
    String reasoning, // 具体的错误点
    String suggestion // 改进建议
) {}
```


## 2. 实现闭环控制逻辑
通过 while 循环控制迭代次数，防止无限循环（Dead Loop）。

``` java

@Service
public class SelfCorrectingPipeline {

    private final ChatClient chatClient;

    public SelfCorrectingPipeline(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public String runSmartWriter(String topic) {
        String currentDraft = "";
        String lastFeedback = "这是初次尝试，请自由发挥。";
        int maxRetries = 3;
        int attempt = 0;

        while (attempt < maxRetries) {
            attempt++;
            System.out.printf("\n--- 第 %d 轮迭代 ---\n", attempt);

            // 1. 生成者 (Writer): 根据反馈进行创作或修改
            currentDraft = chatClient.prompt()
                    .system("你是一个专业的文案专家。")
                    .user(u -> u.text("主题：{topic}\n上次反馈：{feedback}")
                            .param("topic", topic)
                            .param("feedback", lastFeedback))
                    .call()
                    .content();

            // 2. 审查者 (Reviewer): 结构化评估
            ReviewResult review = chatClient.prompt()
                    .system("你是一个极其挑剔的事实核查员，若内容有误必须打回重写。")
                    .user("请审查以下内容：\n" + currentDraft)
                    .call()
                    .entity(ReviewResult.class); // 强类型解析

            System.out.println("核查状态: " + review.status());

            // 3. 逻辑判断
            if ("APPROVED".equalsIgnoreCase(review.status())) {
                System.out.println(">>> 审查通过！");
                return currentDraft;
            } else {
                // 如果不通过，将建议存入 lastFeedback，进入下一次循环
                lastFeedback = "修正建议: " + review.reasoning() + " | " + review.suggestion();
                System.out.println(">>> 审查未通过，准备重写。原因: " + review.reasoning());
            }
        }

        return "在达到最大迭代次数后，最终版本如下（可能仍存在瑕疵）：\n" + currentDraft;
    }
}
```

关键架构点解析:

上下文的滚动传递： 在 while 循环中，lastFeedback 扮演了关键角色。它把上一次失败的原因作为下一次生成的输入。这正是 Agent 反思模式 的精髓。
避免无限循环 (Guardrail)： 代码中设置了 maxRetries。这是生产级 AI 应用必须具备的，防止因为模型陷入逻辑死循环（例如作者和审查员意见始终不一）而耗尽 Token。
状态与提示词分离： 在 Spring AI 中，利用 .param() 注入变量，可以保持 Prompt 模板的整洁，避免了 Python 中字符串拼接可能导致的格式混乱。

## 3. 进阶：如何处理更复杂的逻辑？
如果你的流水线包含更多步骤（例如：提取 -> 写作 -> 核查 -> 翻译），手动写 while 循环可能会变得混乱。这时，Spring 生态提供了更好的选择：

Spring AI + State Machine：如果逻辑非常复杂，可以引入 Spring State Machine。

Advisors（拦截器）：你可以编写一个自定义的 AroundAdvisor，在 writer 执行完后自动调用 reviewer，如果不达标则自动重新触发。
