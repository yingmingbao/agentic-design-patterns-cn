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

# Spring AI Alibaba 是基于 Graph (图) 运行时的。父子关系通过 subAgents 列表或 Task 工具 来建立。

## 1. 自定义非 LLM 行为：实现 BaseAgent 的 Java 等效项
在 Python 中你扩展了 BaseAgent。在 Java 中，你可以通过实现 com.alibaba.cloud.ai.graph.agent.Agent 接口或简单的 Function Calling 来实现自定义逻辑。

``` java
import com.alibaba.cloud.ai.graph.agent.Agent;
import com.alibaba.cloud.ai.graph.AgentRequest;
import com.alibaba.cloud.ai.graph.AgentResponse;
import com.alibaba.cloud.ai.graph.OverAllState;
import reactor.core.publisher.Flux;

/**
 * 对应 Python 中的 TaskExecutor。
 * 在 Spring AI Alibaba 中，可以通过实现子类或定义 Function 来完成。
 */
public class TaskExecutorAgent implements Agent {
    @Override
    public String getName() { return "TaskExecutor"; }

    @Override
    public String getDescription() { return "执行预定义的任务。"; }

    @Override
    public AgentResponse invoke(AgentRequest request) {
        // 自定义逻辑
        return new AgentResponse("任务成功完成。");
    }

    // RC2 版本通常需要实现 invoke 方法，对于流式支持则实现 stream
}
```
## 2. 定义叶子 Agent (Greeter)
使用 ReactAgent 来定义需要 LLM 能力的子 Agent。

``` java

ReactAgent greeter = ReactAgent.builder()
        .chatModel(chatModel)
        .name("Greeter")
        .instruction("你是一个友好的欢迎者。")
        .build();

TaskExecutorAgent taskDoer = new TaskExecutorAgent();
```

## 3. 创建父 Agent (Coordinator) 及其层次结构

在 Python ADK 中，父 Agent 通过 sub_agents 直接管理子 Agent。在 Spring AI Alibaba 中，这种“协调与委托”通常有两种实现方式：

方案 A：使用 SequentialAgent (自动流水线)
如果任务是顺序的（先欢迎，再执行），这是最简单的方案。
``` java
SequentialAgent coordinator = SequentialAgent.builder()
        .name("Coordinator")
        .description("可以欢迎用户并执行任务的协调者。")
        .subAgents(List.of(greeter, taskDoer)) // 建立父子/前后级关系
        .build();

```


方案 B：使用 Task 工具模式 (动态委托)
如果你希望父 Agent 根据用户输入动态决定调用哪个子 Agent（类似 Python 中 instruction 引导的逻辑），目前 RC2 的最佳实践是将子 Agent 包装为 Tool (Function) 挂载给父 Agent。

``` java

// 将子 Agent 包装成工具，让父 Agent 可以动态“调用”它们
ReactAgent coordinator = ReactAgent.builder()
        .chatModel(chatModel)
        .name("Coordinator")
        .instruction("当被要求欢迎时，调用 Greeter 工具。当被要求执行任务时，调用 TaskExecutor 工具。")
        .subAgents(List.of(greeter, taskDoer)) // 在底层注册子 Agent
        .build();
```

## 4. 执行与验证

``` java
AgentRequest request = new AgentRequest("请执行任务并欢迎我");
Optional<OverAllState> result = coordinator.invoke(request);

result.ifPresent(state -> {
    // 验证执行链路（替代 Python 的断言）
    System.out.println("当前运行状态中的消息记录: " + state.data().get("messages"));
});
```


## 总结
自定义逻辑：通过实现 Agent 接口来替代 Python 的 BaseAgent 扩展。

委托层次：Spring AI Alibaba 倾向于使用“状态机”管理。父 Agent 并不像 Python 那样直接拥有子 Agent 的实例引用，而是通过 Graph Runtime 在运行时调度子节点。
