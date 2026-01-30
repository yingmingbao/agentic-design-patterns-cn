package com.strategist.ai.patterns.agenttool;

import com.strategist.ai.patterns.agenttool.component.AgentExecutorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class AgentRunner /*implements CommandLineRunner*/ {


    @Autowired
    private AgentExecutorService agentService;

//    @Override
    public void run(String... args) throws Exception {
        // 同时发起多个查询 (对应 asyncio.gather)
        CompletableFuture<String> task1 = agentService.runAgentAsync("What is the capital of France?");
        CompletableFuture<String> task2 = agentService.runAgentAsync("What's the weather like in London?");
        CompletableFuture<String> task3 = agentService.runAgentAsync("Tell me something about dogs.");
        CompletableFuture<String> task4 = agentService.runAgentAsync("Tell me the current date and time.");

        // 等待所有结果完成并打印
        CompletableFuture.allOf(task1, task2, task3, task4).join();

        System.out.println("\n--- Agent 最终回复 ---");
        System.out.println("回复 1: " + task1.get());
        System.out.println("回复 2: " + task2.get());
        System.out.println("回复 3: " + task3.get());
        System.out.println("回复 4: " + task4.get());
    }
}
