package com.strategist.ai.patterns.prompt;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ExtractionService {

    @Autowired
    private ChatClient.Builder chatClientBuilder;


    private String prompt_extract_template = """
            "请从以下文本中提取技术规格：\\n\\n{text_input}"
            """;
    private String prompt_transform_template = """
            "请将以下技术规格转为JSON 格式，包含'cpu'、'memory' 和'storage' 三个键：\\n\\n{specifications}"
            """;


    public String runFullChain(String rawInput) {

        ChatClient chatClient = chatClientBuilder.build();

        // 1. 第一步：执行 extraction_chain
        String specifications = chatClient.prompt()
                .user(u -> u.text(prompt_extract_template) // 对应 prompt_extract
                        .param("text_input", rawInput))
                .call()
                .content(); // 对应 StrOutputParser()

        // 2. 第二步：执行 full_chain 的后续部分
        return chatClient.prompt()
                .user(u -> u.text(prompt_transform_template) // 对应 prompt_transform
                        .param("specifications", specifications)) // 将上一步结果作为参数
                .call()
                .content(); // 最终输出

    }
}
