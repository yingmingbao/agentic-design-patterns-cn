package com.strategist.ai.patterns.routing.component;

import com.strategist.ai.patterns.routing.SubAgentHandler;
import org.springframework.stereotype.Component;

@Component("info")
public class InfoHandler implements SubAgentHandler {
    public String handle(String request) {
        System.out.println("\n--- 委托给信息处理器 ---");
        return "信息处理器已处理请求：'" + request + "'。结果：模拟信息检索。";
    }
}
