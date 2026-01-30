package com.strategist.ai.patterns.routing;

import com.strategist.ai.patterns.routing.enums.AgentType;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import java.util.Map;

@Component
public class CoordinatorAgent {

    private final ChatClient chatClient;
    private final Map<String, SubAgentHandler> handlers;

    // Spring 会自动将所有 SubAgentHandler 的实现类注入到这个 Map 中
    public CoordinatorAgent(ChatClient.Builder builder, Map<String, SubAgentHandler> handlers) {
        this.chatClient = builder.build();
        this.handlers = handlers;
    }

    public String execute(String userRequest) {
        // 1. 结构化路由决策
        // Spring AI 会自动在 Prompt 后添加格式化指令，并解析返回结果
        AgentType decision = chatClient.prompt()
                .system("""
                    分析用户请求，判断应由哪个专属处理器处理。
                    分类标准：
                    - BOOKER: 涉及预订机票、酒店或行程。
                    - INFO: 一般性的事实查询、百科知识。
                    - UNCLEAR: 请求不明确、超出上述范围或无法处理。
                    """)
                .user(userRequest)
                .call()
                .entity(AgentType.class); // 核心变化：直接获取枚举对象

        System.out.println("系统智能决策结果: " + decision);

        // 2. 更加安全的委托执行
        // 将枚举转为小写字符串去 Map 中匹配 Bean
        String handlerKey = decision.name().toLowerCase();
        SubAgentHandler handler = handlers.getOrDefault(handlerKey, handlers.get("unclear"));
        return handler.handle(userRequest);
    }

}
