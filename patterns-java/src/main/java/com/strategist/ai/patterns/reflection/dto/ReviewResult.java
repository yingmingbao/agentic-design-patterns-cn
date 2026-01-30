package com.strategist.ai.patterns.reflection.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Data;

@Data
public class ReviewResult {

    @JsonPropertyDescription("状态：ACCURATE 或 INACCURATE")
    String status;

    @JsonPropertyDescription("核查的详细逻辑和改进建议")
    String reasoning;

}
