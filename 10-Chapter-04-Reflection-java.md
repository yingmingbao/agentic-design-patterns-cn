# Spring AI Alibaba 实现：反思循环模式

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
