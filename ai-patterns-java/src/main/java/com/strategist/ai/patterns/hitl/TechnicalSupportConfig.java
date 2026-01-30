package com.strategist.ai.patterns.hitl;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@Configuration
public class TechnicalSupportConfig {

    @Bean
    public ChatClient technicalSupportAgent(ChatClient.Builder builder, SupportTools tools) {
        // 模拟初始 State
        Map<String, Object> state = Map.of(
                "customer_info", Map.of(
                        "name", "张三",
                        "tier", "金卡",
                        "recent_purchases", List.of("智能手机", "蓝牙耳机"),
                        "support_history", "曾咨询电池续航问题"
                )
        );

        String systemInstruction = """
                你是一家电子产品公司的技术支持专家。
                首先，检查用户的支持历史。如有，请在回复中引用。
                历史信息：{history}
                
                技术问题处理流程：
                1. 使用 troubleshootIssue 工具分析。
                2. 指导基础排查。
                3. 未解决则调用 createTicket。
                复杂问题直接调用 escalateToHuman。
                保持专业同理心。
                """;

        return builder
                .defaultSystem(
                        s -> s.text(systemInstruction)
                        .param("history", ((Map) state.get("customer_info")).get("support_history")))
                .defaultTools(tools)
                .defaultAdvisors(new PersonalizationAdvisor(state))
                .build();
    }


}
