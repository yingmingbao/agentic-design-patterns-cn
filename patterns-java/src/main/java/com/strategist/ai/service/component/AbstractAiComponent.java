package com.strategist.ai.service.component;

import com.strategist.ai.controller.chat.dto.AiChatDto;
import com.strategist.ai.entity.SysAgentChatDialogDetail;
import com.strategist.ai.enums.RoleEnum;
import com.strategist.ai.service.SysAgentChatDialogDetailService;
import com.strategist.ai.service.SysAgentChatDialogService;
import com.strategist.ai.util.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static com.strategist.ai.enums.RoleEnum.USER;

@Slf4j
public abstract class AbstractAiComponent implements AiServiceComponent {

    @Autowired
    protected SysAgentChatDialogService dialogService;

    @Autowired
    protected SysAgentChatDialogDetailService detailService;

    // Store active chat streams: dialogId -> StreamTask
    protected static final Map<Long, StreamTask> activeChats = new ConcurrentHashMap<>();

    // Abstract method to be implemented by specific models
    protected abstract Flux<String> doStream(List<Message> messages, Long dialogId);

    // Abstract method to get model type (source)
    protected abstract String getModelType();
    
    // Abstract method for blocking call
    protected abstract String doCall(List<Message> messages);

    // Abstract method for title generation
    protected abstract void generateTitleSummary(Long dialogId, String firstMessage);

    @Override
    public String send(AiChatDto params) {
        Long dialogId = Long.parseLong(params.getDialogId().toString());
        String content = StringUtils.isEmpty(params.getContent()) ? "" : params.getContent();

        // 1. Save User Message
        detailService.saveMessage(dialogId, "user", content, getModelType());

        // 2. Prepare Context
        List<Message> messages = prepareMessages(dialogId);

        // 3. Call AI
        String aiReply = doCall(messages);

        // 4. Save AI Reply
        saveAssistantMessage(dialogId, aiReply, getModelType());

        return aiReply;
    }

    @Override
    public SseEmitter createEmitter(AiChatDto params) {
        Long dialogId = Long.parseLong(params.getDialogId().toString());
        String content = StringUtils.isEmpty(params.getContent()) ? "" : params.getContent();

        SseEmitter emitter = new SseEmitter(180000L); // 3 mins timeout

        // Check if there is an active stream for this dialog
        if (activeChats.containsKey(dialogId)) {
            StreamTask task = activeChats.get(dialogId);
            Flux<String> flux = task.sink.asFlux();
            subscribeToFlux(emitter, flux);
            return emitter;
        }

        // If no active stream and no content
        if (content.isEmpty()) {
            try {
                emitter.complete();
            } catch (Exception e) {
                log.error("Error completing SseEmitter: {}", e.getMessage());
            }
            return emitter;
        }

        // 1. Save User Message
        detailService.saveMessage(dialogId, RoleEnum.USER.getRole(), content, getModelType());

        // 2. Prepare Context
        List<SysAgentChatDialogDetail> history = detailService.getDialogHistory(dialogId);

        // Check if this is the first message (history only contains the just-saved user message)
        if (history.size() <= 1) {
            // Async generate title
            generateTitleSummary(dialogId, content);
        }

        List<Message> messages = prepareMessagesFromHistory(history);

        // Create a Sink for this dialog
        Sinks.Many<String> sink = Sinks.many().replay().all();
        StringBuffer fullAnswer = new StringBuffer();

        // Capture context for async thread
        String currentUsername = UserContext.getUsername();
        Long currentUserId = UserContext.getUserId();

        // 3. Start Generation (Async)
        Disposable disposable = doStream(messages, dialogId).subscribe(
                part -> {
                    if (part != null) {
                        fullAnswer.append(part);
                        sink.tryEmitNext(part);
                    }
                },
                throwable -> {
                    // On Error
                    restoreContext(currentUserId, currentUsername);
                    if (activeChats.containsKey(dialogId)) {
                        if (fullAnswer.length() > 0) {
                            saveAssistantMessage(dialogId, fullAnswer.toString(), getModelType());
                        }
                        activeChats.remove(dialogId);
                    }
                    sink.tryEmitError(throwable);
                    UserContext.clear();
                },
                () -> {
                    // On Complete
                    restoreContext(currentUserId, currentUsername);
                    if (activeChats.containsKey(dialogId)) {
                        if (!fullAnswer.isEmpty()) {
                            saveAssistantMessage(dialogId, fullAnswer.toString(), getModelType());
                        }
                        activeChats.remove(dialogId);
                    }
                    sink.tryEmitComplete();
                    UserContext.clear();
                }
        );

        activeChats.put(dialogId, new StreamTask(sink, disposable, fullAnswer));

        // Subscribe the current emitter to the sink
        subscribeToFlux(emitter, sink.asFlux());

        return emitter;
    }

    @Override
    public boolean stop(AiChatDto params) {
        Long dialogId = Long.parseLong(params.getDialogId());
        StreamTask task = activeChats.get(dialogId);

        if (task != null) {
            if (task.disposable != null && !task.disposable.isDisposed()) {
                task.disposable.dispose();
            }

            String partialContent = task.accumulator.toString();
            if (!partialContent.isEmpty()) {
                saveAssistantMessage(dialogId, partialContent, getModelType());
            }

            task.sink.tryEmitComplete();
            activeChats.remove(dialogId);
            return true;
        }
        return false;
    }

    protected void saveAssistantMessage(Long dialogId, String content, String modelType) {
        detailService.saveMessage(dialogId, RoleEnum.ASSISTANT.getRole(), content, modelType);
    }
    protected List<Message> prepareMessages(Long dialogId) {
        List<SysAgentChatDialogDetail> history = detailService.getDialogHistory(dialogId);
        return prepareMessagesFromHistory(history);
    }

    protected List<Message> prepareMessagesFromHistory(List<SysAgentChatDialogDetail> history) {
        List<Message> messages = new ArrayList<>();
        int historyLimit = 10;
        int start = Math.max(0, history.size() - historyLimit);
        for (int i = start; i < history.size(); i++) {
            SysAgentChatDialogDetail detail = history.get(i);
            if (USER.getRole().equals(detail.getRole())) {
                messages.add(new UserMessage(detail.getContent()));
            } else {
                messages.add(new AssistantMessage(detail.getContent()));
            }
        }
        return messages;
    }

    protected void restoreContext(Long userId, String username) {
        UserContext.setUserId(userId);
        UserContext.setUsername(username);
    }

    private void subscribeToFlux(SseEmitter emitter, Flux<String> flux) {
        flux.subscribe(
                part -> {
                    try {
                        Map<String, String> data = new HashMap<>();
                        data.put("content", part);
                        emitter.send(data);
                    } catch (IOException e) {
                        // Client disconnected
                    }
                },
                throwable -> {
                    try {
                        emitter.completeWithError(throwable);
                    } catch (Exception e) {
                    }
                },
                () -> {
                    try {
                        emitter.complete();
                    } catch (Exception e) {
                    }
                }
        );
    }
}
