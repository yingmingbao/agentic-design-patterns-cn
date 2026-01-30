package com.strategist.ai.patterns.reasoning;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

@Service
public class OrchestrationService {

    private final ChatClient rootAgent;

    public OrchestrationService(ChatClient rootAgent) {
        this.rootAgent = rootAgent;
    }

    public void runTask(String userQuery) {
        // 在 RC2 中，通过 ChatClient 的 call() 自动处理多轮工具调用（即自主模式）
        String response = rootAgent.prompt()
                .system("你是总控 Agent，请根据任务决定调用搜索工具或代码工具。")
                .user(userQuery)
                .call()
                .content();

        System.out.println("Final Result: " + response);
    }
}