package com.strategist.ai.patterns.rag;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RagDataService {
    private final VectorStore vectorStore;

    public RagDataService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public void initData() {
        // 1. 加载文档 (模拟 requests.get + TextLoader)
        String content = "President's speech content...";
        Document document = new Document(content);

        // 2. 切分文档 (对应 CharacterTextSplitter)
        TokenTextSplitter splitter = new TokenTextSplitter(500, 50, 5, 1000, true);
        List<Document> chunks = splitter.apply(List.of(document));

        // 3. 写入向量库 (对应 Weaviate)
        vectorStore.add(chunks);
    }
}