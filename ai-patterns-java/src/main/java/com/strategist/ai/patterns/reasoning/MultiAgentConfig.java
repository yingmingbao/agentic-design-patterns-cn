package com.strategist.ai.patterns.reasoning;


import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Configuration
public class MultiAgentConfig {

    // --- 1. 定义子 Agent 工具类 ---
    // --- 1. 子 Agent 工具化 ---
    @Component
    public static class SubAgentTools {
        private final ChatClient searchClient;
        private final ChatClient codeClient;

        public SubAgentTools(ChatClient.Builder builder) {
            // 定义搜索 Agent
            this.searchClient = builder.build();
            // 定义代码 Agent
            this.codeClient = builder.build();
        }

        @Tool(description = "询问搜索专家以获取互联网实时信息")
        public String searchExpert(String query) {
            return searchClient.prompt()
                    .system("你是 Google 搜索专家")
                    .user(query)
                    .call().content();
        }

        @Tool(description = "询问代码专家以编写或执行程序")
        public String codeExpert(String task) {
            return codeClient.prompt()
                    .system("你是代码执行专家")
                    .user(task)
                    .call().content();
        }
    }

    // --- 2. 定义根 Agent (RootAgent) ---

    @Bean
    public ChatClient rootAgent(ChatClient.Builder builder, SubAgentTools tools) {
        return builder
                .defaultSystem("你是总控 Agent。对于复杂任务，请调用搜索专家或代码专家工具。")
                // 核心：注入工具，模型会自动进行多轮 Tool Call 实现自主 Agent 行为
                .defaultTools(tools)
                // 推荐：加入记忆能力，使 Agent 能理解上下文
                .defaultAdvisors(new MessageChatMemoryAdvisor(new InMemoryChatMemory()))
                .build();
    }
}