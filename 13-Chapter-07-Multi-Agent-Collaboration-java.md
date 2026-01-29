# 我们需要利用其 ReactAgent（对应 CrewAI 的 Agent）和 SequentialAgent。

以下是基于 Spring AI Alibaba 1.1.0-RC2 标准的完整 Java 实现。

## 1. 核心依赖配置
确保你的 pom.xml 包含以下核心组件：
``` java
<dependencies>
    <dependency>
        <groupId>com.alibaba.cloud.ai</groupId>
        <artifactId>spring-ai-alibaba-agent-framework</artifactId>
        <version>1.1.0.0-RC2</version>
    </dependency>
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
    </dependency>
</dependencies>
```

## 2. Java 实现：博客创作团队
在 Java 中，我们通过 ReactAgent 定义角色和目标，并使用 SequentialAgent 进行编排。

``` java
import com.alibaba.cloud.ai.graph.AgentRequest;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.SequentialAgent;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class BlogCreationService {

    private final ChatModel chatModel;

    public BlogCreationService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public void createBlogPost(String topic) {
        // 1. 定义高级研究分析师 Agent
        ReactAgent researcher = ReactAgent.builder()
                .chatModel(chatModel)
                .name("高级研究分析师")
                .description("擅长查找并总结 AI 的最新趋势。")
                .instruction("""
                    你是一位经验丰富的研究分析师。
                    你的任务是：研究 2024-2025 年人工智能中出现的前 3 个趋势。
                    重点关注实际应用和潜在影响。
                    输出要求：前 3 个 AI 趋势的详细摘要，包括关键点和来源。
                    """)
                .build();

        // 2. 定义技术内容作家 Agent
        ReactAgent writer = ReactAgent.builder()
                .chatModel(chatModel)
                .name("技术内容作家")
                .description("可以将复杂的技术主题转化为易于理解的博客文章。")
                .instruction("""
                    你是一位熟练的作家。
                    基于研究发现撰写一篇 500 字的博客文章。
                    文章应该引人入胜且易于普通读者理解。
                    最终输出：一篇关于最新 AI 趋势的完整 500 字博客文章。
                    """)
                .build();

        // 3. 编排团队（顺序流）
        SequentialAgent blogCrew = SequentialAgent.builder()
                .name("Blog_Creation_Crew")
                .subAgents(List.of(researcher, writer))
                .build();

        // 4. 执行并获取结果
        System.out.println("## 使用 Gemini 2.0 Flash 运行博客创建团队... ##");
        
        Optional<OverAllState> stateOptional = blogCrew.invoke(new AgentRequest(topic));

        // 5. 提取并打印结果 (基于之前讨论的 data 结构)
        stateOptional.ifPresent(state -> {
            Map<String, Object> data = state.data();
            if (data != null && data.containsKey("messages")) {
                List<Message> messages = (List<Message>) data.get("messages");
                if (!messages.isEmpty()) {
                    String finalOutput = messages.get(messages.size() - 1).getContent();
                    System.out.println("\n------------------\n");
                    System.out.println("## 团队最终输出 ##");
                    System.out.println(finalOutput);
                }
            }
        });
    }
}
```

## 3. 关于“上下文”的补充
在 CrewAI 中，你需要显式地给 writing_task 设置 context=[research_task]。而在 Spring AI Alibaba 的 SequentialAgent 中，它是状态共享模式：

researcher 运行完后，其输出会自动追加到 OverAllState 的 messages 列表中。

writer 启动时，会自动读取列表中的历史信息作为参考上下文。




