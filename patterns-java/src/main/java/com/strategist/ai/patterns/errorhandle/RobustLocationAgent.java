package com.strategist.ai.patterns.errorhandle;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;


@Service
public class RobustLocationAgent {

    private final ChatClient chatClient;

    private final LocationTools locationTools;

    public RobustLocationAgent(ChatClient.Builder builder, LocationTools locationTools) {
        this.chatClient = builder.defaultTools(locationTools).build();
        this.locationTools = locationTools;
    }

    public String execute(String userAddress) {
        // 模拟全局状态 state
        Map<String, Object> state = new HashMap<>();

        // --- 阶段 1: Primary Handler ---
        String primaryPrompt = "你的任务是获取精确的位置信息。请调用 getPreciseLocationInfo，地址是：" + userAddress;
        String primaryResult = chatClient.prompt(primaryPrompt).call().content();

        // 更新状态
        boolean failed = (primaryResult == null || primaryResult.contains("无法获取") || primaryResult.isEmpty());
        state.put("primary_location_failed", failed);
        state.put("location_result", failed ? null : primaryResult);

        // --- 阶段 2: Fallback Handler ---
        if ((Boolean) state.get("primary_location_failed")) {
            String fallbackPrompt = String.format(
                    "主处理器失败。请从地址 '%s' 中提取城市名，并调用 getGeneralAreaInfo。", userAddress);
            String fallbackResult = chatClient.prompt(fallbackPrompt).call().content();
            state.put("location_result", fallbackResult);
        }

        // --- 阶段 3: Response Agent ---
        String resultData = (String) state.get("location_result");
        String finalPrompt = String.format(
                "查看位置信息：'%s'。请清晰简明地向用户展示。若信息为空，请致歉并说明无法获取。",
                resultData == null ? "" : resultData);

        return chatClient.prompt(finalPrompt).call().content();
    }

}
