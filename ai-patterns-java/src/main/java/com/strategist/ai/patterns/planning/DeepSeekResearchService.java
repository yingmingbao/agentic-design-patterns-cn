package com.strategist.ai.patterns.planning;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DeepSeekResearchService {

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    // 确保使用 application.yml 中配置的 deepseek key/url
    // ai.openai.api-key 和 ai.openai.base-url 由 Spring AI 自动配置给 ChatClient
    // 这里我们只需要确保 Prompt 使用正确的模型名称

    public String conductResearch(String query) {
        String systemMessageContent = """
            你是一名专业研究员，需撰写结构化、数据驱动的报告。
            关注数据洞见，使用可靠来源，并在正文中插入引用。
            """;

        // 1. 初始化 ChatClient
        ChatClient chatClient = chatClientBuilder.build();


        // 2. 设置选项 (对应 Python: model="deepseek-reasoner" / "o3-deep-research-...")
        // DeepSeek R1 的推理内容通常在 returning reasoning_content 字段
        ChatOptions options = ChatOptions.builder()

//                .model("DeepSeek-V3.2") // 使用 DeepSeek R1 模型
                .build();

        Prompt prompt = new Prompt(List.of(
                new SystemMessage(systemMessageContent),
                new UserMessage(query)
        ), options);

        // 3. 执行调用
        ChatResponse response = chatClient.prompt(prompt)
                .call()
                .chatResponse();
        Generation generation = response.getResult();
        String finalReport = generation.getOutput().getText();
        
        // 尝试获取推理内容 (DeepSeek API 返回的 reasoning_content)
        // Spring AI 的 Metadata 中可能包含原生响应字段
        String reasoningContent = extractReasoning(generation);

        // --- 打印输出 (模拟 Python 脚本的行为) ---

        // 打印最终报告
        System.out.println(finalReport);

        // --- 获取内嵌引用 (模拟) ---
        System.out.println("--- 引用 ---");
        // 由于 DeepSeek R1 目前主要在文本中生成引用，这里做一个简单的正则提取演示
        // 对应 Python: annotations = response.output[-1].content[0].annotations
        extractAndPrintCitations(finalReport);

        System.out.println("\n" + "=".repeat(50) + "\n");

        // --- 检查中间步骤 ---
        System.out.println("--- 中间步骤 ---");

        // 1. 推理步骤
        if (reasoningContent != null && !reasoningContent.isEmpty()) {
            System.out.println("\n[发现推理步骤]");
            System.out.println("  - " + reasoningContent.replace("\n", "\n  - "));
        } else {
            System.out.println("\n未发现推理步骤。");
        }

        // 2. 网络搜索调用 (DeepSeek R1 可能不直接返回此结构，这里留空或模拟)
        // Python: search_step = next(item for item in response.output if item.type == "web_search_call")
        System.out.println("\n[发现网络搜索调用]");
        System.out.println("  (DeepSeek R1 当前版本主要通过内置知识库或联网插件，API 响应中暂无标准化的 web_search_call 结构返回)");
        // 如果使用了 Function Calling，可以在这里检查 generation.getMetadata().get("tool_calls")

        // 3. 代码执行
        // Python: code_step = next(...)
        System.out.println("\n[发现代码执行步骤]");
        System.out.println("  (DeepSeek R1 API 暂无标准化的 code_interpreter_call 结构返回)");

        return finalReport;
    }

    private String extractReasoning(Generation generation) {
        // 策略 1: 检查 Spring AI Metadata (取决于 Spring AI 版本对 DeepSeek 的适配)
        ChatGenerationMetadata metadata = generation.getMetadata();
        if (metadata != null) {
            // 常见键名可能为 "reasoning_content", "reasoning", 或者在 "raw" / "originalResponse" 中
            if (metadata.containsKey("reasoning_content")) {
                return metadata.get("reasoning_content").toString();
            }
            // 有些实现可能会放在 custom 字段
            if (metadata.containsKey("custom")) {
                Map<?, ?> custom = (Map<?, ?>) metadata.get("custom");
                if (custom != null && custom.containsKey("reasoning_content")) {
                    return custom.get("reasoning_content").toString();
                }
            }
        }
        
        // 策略 2: 如果内容包含 <think> 标签 (DeepSeek 有时会这样输出)
        String content = generation.getOutput().getText();
        if (content.contains("<think>") && content.contains("</think>")) {
            int start = content.indexOf("<think>") + 7;
            int end = content.indexOf("</think>");
            if (start < end) {
                return content.substring(start, end).trim();
            }
        }
        
        return null;
    }

    private void extractAndPrintCitations(String report) {
        // 简单的正则匹配 [1], [2] 等引用标记
        Pattern pattern = Pattern.compile("\\[(\\d+)\\]");
        Matcher matcher = pattern.matcher(report);
        
        boolean found = false;
        while (matcher.find()) {
            found = true;
            String citationRef = matcher.group(0);
            int start = matcher.start();
            int end = matcher.end();
            // 尝试获取上下文
            int contextStart = Math.max(0, start - 20);
            int contextEnd = Math.min(report.length(), end + 20);
            String context = report.substring(contextStart, contextEnd).replace("\n", " ");
            
            System.out.println("引用 " + matcher.group(1) + ":");
            System.out.println("  上下文：" + context);
            System.out.println("  位置：字符 " + start + "–" + end);
        }
        
        if (!found) {
            System.out.println("报告中未发现显式引用标记 (如 [1])。");
        }
    }
}
