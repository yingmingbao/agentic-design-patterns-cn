在该版本中，所谓的“Agent”其实是一个配置了特定指令、工具和 AgentConversationAdvisor 的 ChatClient 实例。

## 1. 核心方案：使用 ChatClient 构建 Agent
在 RC2 中，您不需要寻找 AgentExecutor 或 AgentBuilder。您可以通过以下标准方式实现多智能体协作：

``` java
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Configuration
public class MultiAgentConfig {

    // --- 1. 子 Agent 工具化 ---
    @Component
    public static class SubAgentTools {
        private final ChatClient searchClient;
        private final ChatClient codeClient;

        public SubAgentTools(ChatClient.Builder builder) {
            // 定义搜索 Agent
            this.searchClient = builder.build();
            // 定义代码 Agent
            this.codeClient = builder.build();
        }

        @Tool(description = "询问搜索专家以获取互联网实时信息")
        public String searchExpert(String query) {
            return searchClient.prompt()
                    .system("你是 Google 搜索专家")
                    .user(query)
                    .call().content();
        }

        @Tool(description = "询问代码专家以编写或执行程序")
        public String codeExpert(String task) {
            return codeClient.prompt()
                    .system("你是代码执行专家")
                    .user(task)
                    .call().content();
        }
    }

    // --- 2. 根 Agent 配置 ---
    @Bean
    public ChatClient rootAgent(ChatClient.Builder builder, SubAgentTools tools) {
        return builder
                .defaultSystem("你是总控 Agent。对于复杂任务，请调用搜索专家或代码专家工具。")
                // 核心：注入工具，模型会自动进行多轮 Tool Call 实现自主 Agent 行为
                .defaultTools(tools) 
                // 推荐：加入记忆能力，使 Agent 能理解上下文
                .defaultAdvisors(new MessageChatMemoryAdvisor(new InMemoryChatMemory()))
                .build();
    }
}

```

## 2. 为什么不再需要 AgentExecutor？
在 Spring AI Alibaba 的最新演进中，ChatClient 本身就是一个 Agent 容器：

感知（System Prompt）：通过 .defaultSystem() 定义。

行动（Tools）：通过 .defaultTools() 定义。

记忆（Memory）：通过 Advisor 定义。

循环决策：当模型返回 tool_calls 时，ChatClient 内部会自动执行工具并再次调用模型，直到得到最终回复，这完美替代了 Python 中的 AgentExecutor 循环。

## 3. 如何解决类找不到的问题
如果您希望查看当前版本中具体的 Agent 相关类，建议在 IDE 的依赖库（External Libraries）中展开 spring-ai-alibaba-agent-framework，通常路径如下：

任务定义：com.alibaba.cloud.ai.agent.task.AgentTask

常用工具适配器：com.alibaba.cloud.ai.agent.tool.*

## 4. 关键验证步骤
请确保您的 pom.xml 中引入了核心启动器，它会包含所有必要的 Advisor：

``` xml
<dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>spring-ai-alibaba-starter</artifactId>
    <version>1.0.0-M6</version>
</dependency>
```
