package com.strategist.ai.patterns.reflection;

import com.strategist.ai.patterns.reflection.dto.ReviewResult;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class ReviewPipelineService {

    @Resource
    private final ChatClient chatClient;

    public ReviewPipelineService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public ReviewResult runWriteAndReview(String topic) {
        // --- 1. 执行 DraftWriter (生成初稿) ---
        String draftText = chatClient.prompt()
                .system("你是一个专业的初稿撰写员，请写一段简短、信息丰富的主题段落。")
                .user(topic)
                .call()
                .content();

        System.out.println(">>> 初稿已生成: " + draftText);

        // --- 2. 执行 FactChecker (事实核查并结构化输出) ---
        // 我们直接将上一步的 draftText 传递给审查员
        return chatClient.prompt()
                .system("你是一名严谨的事实核查员。请核查用户提供的文本。")
                .user("待核查文本：\n" + draftText)
                .call()
                .entity(ReviewResult.class); // 核心：自动转换为 Java 对象
    }
}
