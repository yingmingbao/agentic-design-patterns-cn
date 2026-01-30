package com.strategist.ai.patterns.reflection;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class ReflectionService {

    private final ChatClient chatClient;

    public ReflectionService(ChatClient.Builder builder) {
        // 我们不在这里绑定默认记忆，因为我们需要手动管理生成者和反思者的上下文
        this.chatClient = builder.build();
    }

    private String task_prompt = """
            你的任务是创建一个名为 `calculate_factorial` 的 Python 函数。
            该函数需满足以下要求：
            1. 只接受一个整数参数 n。
            2. 计算其阶乘（n!）。
            3. 包含清晰的 docstring，说明函数功能。
            4. 处理边界情况：0 的阶乘为 1。
            5. 处理无效输入：若输入为负数则抛出 ValueError。
            """;

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

            assert response != null;
            currentCode = response.getResult().getOutput().getText();
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


    public void run(String... args) throws Exception {
        this.runReflectionLoop(task_prompt, 5);
    }
}
