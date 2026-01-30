package com.strategist.ai.patterns.hitl;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SupportTools {

    @Tool(description = "分析并提供故障排查步骤")
    public Map<String, String> troubleshootIssue(String issue) {
        return Map.of("status", "success", "report", "排查步骤：" + issue);
    }

    @Tool(description = "为未解决的问题创建工单")
    public Map<String, String> createTicket(String issueType, String details) {
        return Map.of("status", "success", "ticket_id", "TICKET123");
    }

    @Tool(description = "将复杂问题升级给人工专家")
    public Map<String, String> escalateToHuman(String issueType) {
        return Map.of("status", "success", "message", issueType + " 已升级至专家队列。");
    }
}