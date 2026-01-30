package com.strategist.ai.patterns.rag;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.stereotype.Service;

@Service
public class RagGraphService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    public RagGraphService(ChatClient.Builder builder, VectorStore vectorStore) {
        this.vectorStore = vectorStore;
        // 使用内置的 QuestionAnswerAdvisor 替代手动节点流转
        this.chatClient = builder
                .defaultSystem("You are an assistant for question-answering tasks.\n" +
                        "Use the following retrieved context to answer. Maximum three sentences.")
                .defaultAdvisors(new QuestionAnswerAdvisor(vectorStore, SearchRequest.builder().build()))
                .build();
    }

    public String ask(String question) {
        // 这里的 call() 内部自动完成了：
        // 1. 向量检索 (Retrieve)
        // 2. 填充上下文 (Augment)
        // 3. 模型调用 (Generate)
        return chatClient.prompt(question).call().content();
    }
}