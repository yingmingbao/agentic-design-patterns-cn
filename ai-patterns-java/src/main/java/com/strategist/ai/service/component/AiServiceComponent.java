package com.strategist.ai.service.component;

import com.strategist.ai.controller.chat.dto.AiChatDto;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface AiServiceComponent {
    String send(AiChatDto params);
    SseEmitter createEmitter(AiChatDto params);
    boolean stop(AiChatDto params);
}
