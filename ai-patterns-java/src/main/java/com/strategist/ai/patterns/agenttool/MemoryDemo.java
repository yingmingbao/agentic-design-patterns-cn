package com.strategist.ai.patterns.agenttool;

import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.strategist.ai.patterns.agenttool.component.StatefulAgentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class MemoryDemo /* implements CommandLineRunner*/ {


    @Autowired
    private StatefulAgentService agent;

//    @Override
    public void run(String... args) throws GraphRunnerException {
        String sessionId = "user-123";

        // 第一轮：触发工具调用
        System.out.println("User: 伦敦现在的天气怎么样？");
        System.out.println("AI: " + agent.chat(sessionId, "伦敦现在的天气怎么样？"));
        // 输出：伦敦当前天气多云，气温 15°C。

        // 第二轮：依赖第一轮的记忆（指代“那里”）
        System.out.println("\nUser: 那里的人口是多少？");
        System.out.println("AI: " + agent.chat(sessionId, "那里的人口是多少？"));
        // AI 会理解“那里”指代伦敦，并再次调用工具查询人口。
    }
}