package com.strategist.ai.service.component;

import com.strategist.ai.service.enums.AIChatEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component("deepSearchComponent")
public class DeepSearchComponent extends AbstractAiComponent {

    @Resource
    private DeepResearchAgentService deepResearchAgentService;

    @Override
    protected Flux<String> doStream(List<Message> messages, Long dialogId) {
        // Extract the last user message as the topic
        String topic = messages.stream()
                .filter(m -> m instanceof UserMessage)
                .reduce((first, second) -> second)
                .map(Message::getText)
                .orElse("Unknown Topic");

        return Flux.create(sink -> {
            AtomicBoolean thinkingStarted = new AtomicBoolean(false);

            deepResearchAgentService.runDeepResearchStream(topic, dialogId).subscribe(
                    sse -> {
                        String data = sse.data();
                        if (data == null) return;

                        if ("thinking".equals(sse.event())) {
                            if (!thinkingStarted.get()) {
                                sink.next("<think>\n");
                                thinkingStarted.set(true);
                            }
                            sink.next(data);
                        } else {
                            if (thinkingStarted.get()) {
                                sink.next("\n</think>\n");
                                thinkingStarted.set(false);
                            }
                            sink.next(data);
                        }
                    },
                    sink::error,
                    () -> {
                        if (thinkingStarted.get()) {
                            sink.next("\n</think>\n");
                        }
                        sink.complete();
                    }
            );
        });
    }

    @Override
    protected void saveAssistantMessage(Long dialogId, String content, String modelType) {
        // Do nothing, because DeepResearchAgentService handles saving internally
        // with separated reasoning_content and content fields.
    }

    @Override
    protected String getModelType() {
        return AIChatEnum.deepsearch.getName();
    }

    @Override
    protected String doCall(List<Message> messages) {
        // Fallback to blocking call if needed, or reuse stream blocking
        // Extract the last user message as the topic
        String topic = messages.stream()
                .filter(m -> m instanceof UserMessage)
                .reduce((first, second) -> second)
                .map(Message::getText)
                .orElse("Unknown Topic");
        
        try {
            return deepResearchAgentService.runDeepResearch(topic);
        } catch (Exception e) {
            log.error("Deep research blocking call failed", e);
            return "Deep research failed: " + e.getMessage();
        }
    }

    @Override
    protected void generateTitleSummary(Long dialogId, String firstMessage) {
        // Simple title generation or reuse existing logic
        // For now, we can just set the topic as title or call a helper
        // Since we don't have the helper injected here easily, we'll skip or implement simple one
        // AbstractAiComponent doesn't implement it, so we must.
        
        // We can reuse DeepSeekComponent's logic if we want, but simpler to just update title to first message (truncated)
        String title = firstMessage.length() > 20 ? firstMessage.substring(0, 20) + "..." : firstMessage;
        
        com.strategist.ai.entity.SysAgentChatDialog dialog = new com.strategist.ai.entity.SysAgentChatDialog();
        dialog.setId(dialogId);
        dialog.setTitle(title);
        dialogService.updateById(dialog);
    }
}
