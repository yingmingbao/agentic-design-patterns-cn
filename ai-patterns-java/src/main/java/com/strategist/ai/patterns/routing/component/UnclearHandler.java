package com.strategist.ai.patterns.routing.component;

import com.strategist.ai.patterns.routing.SubAgentHandler;
import org.springframework.stereotype.Component;

@Component("unclear")
public class UnclearHandler implements SubAgentHandler {
    public String handle(String request) {
        System.out.println("\n--- 处理不明确请求 ---");
        return "协调者无法委托请求：'" + request + "'。请补充说明。";
    }
}
