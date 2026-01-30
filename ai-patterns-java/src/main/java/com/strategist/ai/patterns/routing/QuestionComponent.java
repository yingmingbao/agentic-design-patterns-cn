package com.strategist.ai.patterns.routing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;


@Component
public class QuestionComponent {


    @Autowired
    private CoordinatorAgent coordinator;

   // @Override
    public void run(String... args) throws Exception {
        System.out.println("--- 预订请求示例 ---");
        System.out.println("最终结果 A: " + coordinator.execute("帮我预订飞往伦敦的机票。"));

        System.out.println("\n--- 信息请求示例 ---");
        System.out.println("最终结果 B: " + coordinator.execute("意大利的首都是哪里？"));

        System.out.println("\n--- 不明确请求示例 ---");
        System.out.println("最终结果 C: " + coordinator.execute("讲讲量子物理。"));
    }
}
