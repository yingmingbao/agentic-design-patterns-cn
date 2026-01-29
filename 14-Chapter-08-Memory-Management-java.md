## memory
 https://github.com/yingmingbao/agentic-design-patterns-cn/blob/agentic-design-patterns-cn-java/11-Chapter-05-Tool-Use-java.md#%E5%9C%A8-spring-ai-alibaba-%E4%B8%AD%E4%B8%BA-agent-%E5%8A%A0%E5%85%A5%E8%AE%B0%E5%BF%86%E5%8A%9F%E8%83%BD

# 在 Spring AI Alibaba 1.1.0-RC2 框架中，要实现 Python ADK 中 SessionService 的 Redis 持久化效果，你需要配置 StateRepository 的 Redis 实现。

在 Java 中，状态（State）管理是自动化的：通过 threadId 标识会话，框架会自动将 OverAllState（包含你定义的 outputKey）持久化到 Redis。

## 1. 添加 Redis 持久化依赖
在你的 pom.xml 中，除了基础的 Agent 框架，还需要加入 Spring Data Redis：
``` xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>spring-ai-alibaba-graph</artifactId>
    <version>1.1.0.0-RC2</version>
</dependency>
```

## 2. 配置 Redis 持久化 (application.yml)
你需要告知 Spring AI Alibaba 使用 Redis 作为 Graph 的状态后端。

``` yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
  ai:
    alibaba:
      graph:
        persistence:
          type: redis # 切换为 redis 存储，替代默认的 memory
    openai:
      base-url: https://api.deepseek.com # 或 Gemini 端点
      api-key: ${DEEPSEEK_API_KEY}
```

## 3. Java 实现：带有 Redis 会话管理的 Agent

``` java
import com.alibaba.cloud.ai.graph.AgentRequest;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.StateRepository;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class RedisStateAgentService {

    private final ChatModel chatModel;
    
    @Autowired
    private StateRepository stateRepository; // 注入状态仓库，用于手动检查 Redis 状态

    public RedisStateAgentService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public void executePersistentTask(String userId, String sessionId) {
        // 1. 定义 Agent 并指定 outputKey
        ReactAgent greetingAgent = ReactAgent.builder()
                .chatModel(chatModel)
                .name("Greeter")
                .instruction("Generate a short, friendly greeting.")
                .outputKey("last_greeting") // 结果会存入 state.data("last_greeting")
                .build();

        // 2. 将 Python 的 app:user:session 映射为 ThreadId
        // Spring AI Alibaba 使用 ThreadId 作为 Redis 的 Key 标识
        String threadId = String.format("%s:%s", userId, sessionId);

        // 3. 运行 Agent
        System.out.println("--- Running the agent with Redis Persistence ---");
        AgentRequest request = AgentRequest.builder()
                .query("Hello")
                .threadId(threadId)
                .build();

        // invoke 执行后，状态已自动同步到 Redis
        Optional<OverAllState> responseState = greetingAgent.invoke(request);

        // 4. 检查 Redis 中更新后的状态
        // 即使重启应用，通过此 threadId 依然能找回状态
        checkRedisState(threadId);
    }

    private void checkRedisState(String threadId) {
        // 从 Redis 仓库直接读取最新状态
        Optional<OverAllState> savedState = stateRepository.getLatestState(threadId);
        
        savedState.ifPresent(state -> {
            System.out.println("\n[Redis State Found]");
            // 获取我们定义的 outputKey 值
            Object lastGreeting = state.data().get("last_greeting");
            System.out.println("last_greeting -> " + lastGreeting);
            
            // 打印消息历史条数
            System.out.println("History size in Redis: " + state.getMessages().size());
        });
    }
}
```
## 4. 为什么在 Redis 中找不到数据？
如果你在 Redis 终端执行 KEYS * 没看到数据，请检查：

序列化配置：Spring AI Alibaba 默认使用 JSON 序列化。如果你的 OverAllState 中包含无法序列化的对象，可能会报错。

Bean 冲突：确保没有自定义其他的 StateRepository 覆盖了 Redis 实现。

ThreadId 传递：在 invoke 时必须传入 threadId，否则 Agent 会运行在“匿名模式”下，不触发持久化。
