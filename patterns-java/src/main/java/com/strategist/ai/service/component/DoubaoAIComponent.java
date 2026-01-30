package com.strategist.ai.service.component;

import com.strategist.ai.service.enums.AIChatEnum;
import com.strategist.ai.util.UserContext;
import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionRequest;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessage;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessageRole;
import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionChunk;
import com.volcengine.ark.runtime.service.ArkService;
import io.reactivex.Flowable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
@Slf4j
public class DoubaoAIComponent extends AbstractAiComponent {

    private ArkService arkService;

    @Value("${volcengine.doubao.apikey}")
    private String apiKey;

    @Value("${volcengine.doubao.model}")
    private String model;

    @Value("${volcengine.doubao.base-url}")
    private String baseUrl;


    private final String API_KEY = "575ffaf6-f885-4f91-997e-55226426ead2";
    private final String MODEL_ID = "doubao-seed-1-8-251228"; // Using a valid model ID from user context or default

    @PostConstruct
    public void init() {
        this.arkService = ArkService.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .build();
    }

    @Override
    protected Flux<String> doStream(List<Message> messages, Long dialogId) {
        try {
            List<ChatMessage> doubaoMessages = convertMessages(messages);

            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(model)
                    .messages(doubaoMessages)
                    .build();

            return Flux.from(arkService.streamChatCompletion(request))
                    .map(chunk -> {
                        if (chunk.getChoices() != null && !chunk.getChoices().isEmpty()) {
                            Object content = chunk.getChoices().get(0).getMessage().getContent();
                            return content != null ? content.toString() : "";
                        }
                        return "";
                    })
                    .filter(text -> !text.isEmpty());
        } catch (Exception e) {
            log.error("Doubao stream error", e);
            return Flux.error(e);
        }
    }

    @Override
    protected String getModelType() {
        return AIChatEnum.doubao.getName();
    }

    @Override
    protected String doCall(List<Message> messages) {
        List<ChatMessage> doubaoMessages = convertMessages(messages);

        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model(MODEL_ID)
                .messages(doubaoMessages)
                .build();

        return arkService.createChatCompletion(request).getChoices().get(0).getMessage().getContent().toString();
    }

    private List<ChatMessage> convertMessages(List<Message> messages) {
        List<ChatMessage> doubaoMessages = new ArrayList<>();
        for (Message msg : messages) {
            ChatMessageRole role = ChatMessageRole.USER;
            if (msg.getMessageType() == MessageType.ASSISTANT) {
                role = ChatMessageRole.ASSISTANT;
            } else if (msg.getMessageType() == MessageType.SYSTEM) {
                role = ChatMessageRole.SYSTEM;
            }

            doubaoMessages.add(ChatMessage.builder()
                    .role(role)
                    .content(msg.getText())
                    .build());
        }
        return doubaoMessages;
    }

    @Override
    @Async
    public void generateTitleSummary(Long dialogId, String firstMessage) {
        Long userId = UserContext.getUserId();
        String username = UserContext.getUsername();

        CompletableFuture.runAsync(() -> {
            restoreContext(userId, username);
            try {
                String promptText = "请总结以下对话内容，生成一个简短的标题（不超过15个字），不要包含任何标点符号：" + firstMessage;
                List<ChatMessage> messages = new ArrayList<>();
                messages.add(ChatMessage.builder().role(ChatMessageRole.USER).content(promptText.toString()).build());

                ChatCompletionRequest request = ChatCompletionRequest.builder()
                        .model(MODEL_ID)
                        .messages(messages)
                        .build();

                String title = arkService.createChatCompletion(request).getChoices().get(0).getMessage().getContent().toString();
                if (title != null && !title.isEmpty()) {
                    title = title.replaceAll("^[\"']+|[\"']+$", "").trim();
                    dialogService.updateTitle(dialogId, title);
                }
            } catch (Exception e) {
                log.info("Failed to generate title: " + e.getMessage());
            } finally {
                UserContext.clear();
            }
        });
    }
}
