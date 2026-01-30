package com.strategist.ai.patterns.planning.dto;

import lombok.Data;

@Data
public class AgentRequest {
    private String query;

    public AgentRequest(String s) {
        this.query = s;
    }
}
