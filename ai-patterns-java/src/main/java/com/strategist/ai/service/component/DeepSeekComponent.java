package com.strategist.ai.service.component;

import com.strategist.ai.service.enums.AIChatEnum;
import com.strategist.ai.util.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
@Primary
@Slf4j
public class DeepSeekComponent extends AbstractAiComponent {

    @Autowired
    private ChatModel chatModel;

    @Override
    protected Flux<String> doStream(List<Message> messages, Long dialogId) {
        Prompt prompt = new Prompt(messages);
        return chatModel.stream(prompt)
                .map(chatResponse -> {
                    String text = chatResponse.getResult().getOutput().getText();
                    return text != null ? text : "";
                });
    }

    @Override
    protected String getModelType() {
        return AIChatEnum.deepseek.getName();
    }

    @Override
    protected String doCall(List<Message> messages) {
        Prompt prompt = new Prompt(messages);
        ChatResponse response = chatModel.call(prompt);
        return response.getResult().getOutput().getText();
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
                Prompt prompt = new Prompt(promptText);
                ChatResponse response = chatModel.call(prompt);
                String title = response.getResult().getOutput().getText();
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
