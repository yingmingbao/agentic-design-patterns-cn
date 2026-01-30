package com.strategist.ai.patterns.planning.dto;


import lombok.Data;

import java.util.List;

@Data
public class ResearchResponse {
    private List<OutputItem> output;

    public ResearchResponse(List<OutputItem> output) {
        this.output = output;
    }
}
