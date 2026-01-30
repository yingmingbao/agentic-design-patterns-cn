
# Goal-Setting-And-Monitoring-java


## 1. 核心架构设计

在 Spring AI 中，我们不再手动拼接字符串，而是使用 PromptTemplate 和结构化的 Agent 组件。

## 2. Java 代码实现

A. 配置类 

首先，配置 ChatClient 和基础 Bean。

### GoalSettingDTO

``` java
import com.alibaba.fastjson.JSON;
import lombok.Data;

@Data
public class GoalSettingDTO {
    String useCase;
    String goalsInput;
    int maxIterations;

    public static void main(String[] args) {
        GoalSettingDTO dto = new GoalSettingDTO();
        dto.goalsInput="Write code to find BinaryGap of a given positive integer";
        dto.useCase="Code simple to understand, Functionally correct, Handles comprehensive edge cases, Takes positive integer input only, prints the results with few examples";
        dto.maxIterations = 5;
        System.out.println(JSON.toJSONString(dto));

        dto.goalsInput="Write code to count the number of files in current directory and all its nested sub directories, and print the total count";
        dto.useCase="Code simple to understand, Functionally correct, Handles comprehensive edge cases, Ignore recommendations for performance, Ignore recommendations for test suite use like unittest or pytest";
        System.out.println(JSON.toJSONString(dto));

        dto.goalsInput="Write code which takes a command line input of a word doc or docx file and opens it and counts the number of words, and characters in it and prints all";
        dto.useCase="Code simple to understand, Functionally correct, Handles edge cases";

        System.out.println(JSON.toJSONString(dto));

    }
}
```
### CodeAgentService
 
``` java

import create... GoalSettingDTO;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

@Service
public class CodeAgentService {

    private final ChatClient chatClient;

    public CodeAgentService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public String runCodeAgent(GoalSettingDTO goalSettingDTO) throws IOException {
        List<String> goals = Arrays.stream(goalSettingDTO.getGoalsInput().split(",")).map(String::trim).toList();
        String previousCode = "";
        String feedback = "";

        System.out.println("\n🎯 Use Case: " + goalSettingDTO.getUseCase());

        for (int i = 0; i < goalSettingDTO.getMaxIterations(); i++) {
            System.out.printf("\n=== 🔁 Iteration %d of %d ===\n", i + 1, goalSettingDTO.getMaxIterations());

            // 1. 生成代码 (Generate)
            String code = generateCode(goalSettingDTO.getUseCase(), goals, previousCode, feedback);
            System.out.println("🧾 Generated Code Preview:\n" + code.lines().limit(5).reduce("", (a, b) -> a + "\n" + b));

            // 2. 获取反馈 (Critique)
            feedback = getFeedback(code, goals);
            System.out.println("\n📥 Feedback Received:\n" + feedback);

            // 3. 检查目标是否达成 (Review)
            if (isGoalsMet(feedback, goals)) {
                System.out.println("✅ Goals met. Saving file...");
                saveCodeToFile(code, goalSettingDTO.getUseCase());
                return "success";
            }

            previousCode = code;
            System.out.println("🛠️ Refining in next iteration...");
        }
        return "complete";
    }

    private String generateCode(String useCase, List<String> goals, String prevCode, String feedback) {
        String template = """
                You are a Java coding agent. Write Java code based on: {useCase}
                Goals: {goals}
                {prevCodeSection}
                {feedbackSection}
                Please return ONLY the code, no explanations.
                """;

        Map<String, Object> model = new HashMap<>();
        model.put("useCase", useCase);
        model.put("goals", goals);
        model.put("prevCodeSection", prevCode.isEmpty() ? "" : "Previously generated code:\n" + prevCode);
        model.put("feedbackSection", feedback.isEmpty() ? "" : "Feedback on previous version:\n" + feedback);

        // PromptTemplate promptTemplate = new PromptTemplate(template);
        // return cleanCode(chatClient.prompt(promptTemplate.create(model)).call().content());

        return cleanCode(Objects.requireNonNull(
                        chatClient.prompt()
                                .user(u -> u.text(template).params(model)) // 直接在这里绑定模板和参数
                                .call()
                                .content()
                )
        );
    }

    private String getFeedback(String code, List<String> goals) {
        String template = """
                Critique this Java code based on goals: {goals}
                Code:
                {code}
                """;
//        return chatClient.prompt(new PromptTemplate(template, Map.of("goals", goals, "code", code))).call().content();
        return cleanCode(Objects.requireNonNull(
                        chatClient.prompt()
                                .user(u -> u.text(template).params(Map.of("goals", goals, "code", code))) // 直接在这里绑定模板和参数
                                .call()
                                .content()
                )
        );

    }

    private boolean isGoalsMet(String feedback, List<String> goals) {
        String template = "Based on this feedback: '{feedback}', are the goals {goals} met? Reply with ONLY 'True' or 'False'.";

        String response = cleanCode(
                Objects.requireNonNull(
                        chatClient.prompt()
                                .user(u -> u.text(template).params(Map.of("feedback", feedback, "goals", goals))) // 直接在这里绑定模板和参数
                                .call()
                                .content()
                )
        );
        return response.contains("true");
    }

    private String cleanCode(String raw) {
        return raw.replaceAll("```java|```", "").trim();
    }

    private void saveCodeToFile(String code, String useCase) throws IOException {
        String fileName = useCase.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();

        if (fileName.length() > 15) {
            fileName = fileName.substring(0, 15);
        }

        fileName += "_" + new Random().nextInt(1000, 9999) + ".java";

        Files.writeString(Paths.get(fileName), "// Use Case: " + useCase + "\n" + code);
        System.out.println("✅ Saved to: " + fileName);
    }
}

```

## 3. 为什么使用 spring-ai-alibaba-agent-framework？
虽然上面的代码为了逻辑清晰使用了手动循环，但在引入你提到的 agent-framework 后，你可以将生成、评审和保存定义为独立的 Tools (函数)。
