package com.strategist.ai.controller.chat.dto;

import lombok.Data;

@Data

public class AiChatDto {
    private String content;
    private String dialogId;
    // chose one of llm type
    private String type;
}
