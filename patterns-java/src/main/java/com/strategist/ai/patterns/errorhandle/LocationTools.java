package com.strategist.ai.patterns.errorhandle;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class LocationTools {

    @Tool(description = "获取精确的位置详细信息")
    public String getPreciseLocationInfo(String address) {
        // 模拟精确查找逻辑
        return null; // 假设查找失败返回null
    }

    @Tool(description = "获取城市大致区域信息")
    public String getGeneralAreaInfo(String city) {
        return "城市：" + city + " 的通用区域信息...";
    }
}