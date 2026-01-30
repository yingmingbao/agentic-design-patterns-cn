package com.strategist.ai.patterns.rag;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.stereotype.Component;

@Component
public class RunQuery {

    private ChatClient chatClient;

    RunQuery(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public void runQuery() {
        String query = "What did the president say about Justice Breyer?";

        // 模拟 Python 的 stream 输出
        chatClient.prompt(query)
                .advisors(new SimpleLoggerAdvisor()) // 增加日志记录，模拟节点流转输出
                .stream()
                .content()
                .subscribe(System.out::print);
    }


}
