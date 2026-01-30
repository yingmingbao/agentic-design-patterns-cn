package com.strategist.ai.patterns.planning.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class OutputItem {
    String type;
    List<ContentItem> content;
    List<ReasoningSummary> summary; // 仅 type="reasoning" 时有值
    Map<String, Object> action;      // 仅 type="web_search_call" 时有值
    String input;                  // 仅 type="code_interpreter_call" 时有值
    String status;
}
