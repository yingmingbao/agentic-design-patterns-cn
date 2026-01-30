package com.strategist.ai.patterns.hitl;

import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.AdvisedRequest;
import org.springframework.ai.chat.client.advisor.api.AdvisedResponse;

import java.util.Map;


public class PersonalizationAdvisor implements CallAroundAdvisor {

    private final Map<String, Object> customerState;

    public PersonalizationAdvisor(Map<String, Object> customerState) {
        this.customerState = customerState;
    }

    @Override
    public AdvisedResponse aroundCall(AdvisedRequest advisedRequest, CallAroundAdvisorChain chain) {
        // 模拟 Python 中的 personalization_callback 逻辑
        Map<String, Object> info = (Map<String, Object>) customerState.get("customer_info");

        String note = String.format(
                """
                        重要个性化信息：
                        客户姓名：%s
                        客户等级：%s
                        最近购买：%s
                        """,
                info.getOrDefault("name", "尊贵客户"),
                info.getOrDefault("tier", "标准"),
                info.getOrDefault("recent_purchases", "无")
        );

        // 将个性化信息注入到 User Message 之前或作为 System Prompt 补充
        AdvisedRequest modifiedRequest = AdvisedRequest.from(advisedRequest)
                .userText(advisedRequest.userText() + note)
                .build();

        return chain.nextAroundCall(modifiedRequest);
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @NotNull
    @Override
    public String getName() {
        return "PersonalizationAdvisor";
    }
}