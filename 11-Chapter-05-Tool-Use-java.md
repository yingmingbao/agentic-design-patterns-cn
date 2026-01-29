# ReAct (Reasoning and Acting) 智能体。
它利用模型的工具调用（Tool Calling）能力，在推理过程中自主决定是否调用外部工具。

在 Spring AI Alibaba 中，实现这一模式非常直观。Spring AI 将工具调用抽象为 Function Calling，通过简单的 Bean 注册即可让 AI 自动发现并使用工具。

# Spring AI Alibaba 实现：Tool Calling 智能体模式

## 1. 定义工具 (Spring AI Functions)
在 Java 中，我们将工具定义为带有 @Description 的 Function。这个描述信息就是模型决定是否调用该工具的关键依据。
``` java
@Configuration
public class AgentToolConfig {

    @Bean
    @Description("根据主题提供事实信息。用于回答如“法国首都”或“伦敦天气”等问题。")
    public Function<SearchRequest, SearchResponse> searchInformation() {
        return request -> {
            String query = request.query().toLowerCase();
            System.out.println("\n--- 🛠️ 工具调用：searchInformation, 查询：'" + query + "' ---");
            
            // 模拟预设结果
            String result = switch (query) {
                case "weather in london" -> "伦敦当前天气多云，气温 15°C。";
                case "capital of france" -> "法国的首都是巴黎。";
                case "population of earth" -> "地球人口约 80 亿。";
                default -> "模拟搜索 '" + query + "'：未找到具体信息，但该主题很有趣。";
            };
            
            return new SearchResponse(result);
        };
    }

    public record SearchRequest(String query) {}
    public record SearchResponse(String answer) {}
}
```


## 2. 实现并发 Agent 执行器

我们使用 ChatClient 并开启函数调用功能。

``` java
@Service
public class AgentExecutorService {

    private final ChatClient chatClient;

    public AgentExecutorService(ChatClient.Builder builder) {
        // 绑定定义的工具 Bean 名称
        this.chatClient = builder
                .defaultFunctions("searchInformation")
                .build();
    }

    // 异步执行查询，模拟 asyncio.gather
    public CompletableFuture<String> runAgentAsync(String query) {
        return CompletableFuture.supplyAsync(() -> {
            System.out.println("\n--- 🏃 Agent 运行查询：'" + query + "' ---");
            
            // Spring AI 会自动处理推理、工具调用、再推理的循环
            return chatClient.prompt()
                    .system("你是一个乐于助人的助手。")
                    .user(query)
                    .call()
                    .content();
        });
    }
}
```

## 3. 主程序与并发运行

``` java
@Component
public class AgentRunner implements CommandLineRunner {

    @Autowired
    private AgentExecutorService agentService;

    @Override
    public void run(String... args) throws Exception {
        // 同时发起多个查询 (对应 asyncio.gather)
        CompletableFuture<String> task1 = agentService.runAgentAsync("What is the capital of France?");
        CompletableFuture<String> task2 = agentService.runAgentAsync("What's the weather like in London?");
        CompletableFuture<String> task3 = agentService.runAgentAsync("Tell me something about dogs.");

        // 等待所有结果完成并打印
        CompletableFuture.allOf(task1, task2, task3).join();

        System.out.println("\n--- ✅ Agent 最终回复 ---");
        System.out.println("回复 1: " + task1.get());
        System.out.println("回复 2: " + task2.get());
        System.out.println("回复 3: " + task3.get());
    }
}
```

## 为什么 Spring AI 的实现更简洁？
自动流水线：在 LangChain 中，你需要显式创建 AgentExecutor。在 Spring AI 中，只要在 ChatClient 中配置了 defaultFunctions，它在底层会自动执行“思考-调用-观察”的循环，直到得出最终答案。
解耦与注入：工具（Function）是标准的 Spring Bean，这意味着你可以轻松地在工具中注入数据库连接、Redis 缓存或其他微服务客户端。
强类型入参：Java 的 record 自动定义了工具需要的 JSON Schema。当 AI 准备调用工具时，Spring AI 会自动将 AI 生成的参数映射为 SearchRequest 对象，省去了 Python 中手动解析字符串的麻烦。


## 在 Spring AI Alibaba 中，为 Agent 加入记忆功能

你不需要手动管理复杂的对话历史列表，只需要通过 Advisor（顾问） 机制挂载一个 ChatMemory 即可。
这样，Agent 就能在后续对话中回想起之前的搜索结果（比如它刚查过的巴黎天气或伦敦人口）。

实现方案：带有记忆功能的 Tool Calling Agent

## 1. 配置对话记忆 Bean
``` java
@Configuration
public class AiConfig {
    @Bean
    public ChatMemory chatMemory() {
        // 使用内存存储，记录最近的对话
        return new InMemoryChatMemory();
    }
}
```
首先，我们需要一个存储容器。在开发环境下可以使用内存存储，生产环境则通常对接 Redis。

## 2. 升级 Agent 服务

``` java
@Service
public class StatefulAgentService {

    private final ChatClient chatClient;

    public StatefulAgentService(ChatClient.Builder builder, ChatMemory chatMemory) {
        this.chatClient = builder
                // 1. 绑定工具
                .defaultFunctions("searchInformation")
                // 2. 挂载记忆顾问，chat-id 用于区分不同用户的会话
                .defaultAdvisors(new MessageChatMemoryAdvisor(chatMemory))
                .build();
    }

    public String chat(String chatId, String userQuery) {
        return chatClient.prompt()
                .system("你是一个记性很好的助手。")
                .user(userQuery)
                // 传入会话 ID，这样 AI 就能找到属于该用户的历史记录
                .advisors(a -> a.param(AbstractChatMemoryAdvisor.CHAT_ID_CONVERSATION_ID_KEY, chatId))
                .call()
                .content();
    }
}
```

## 3. 连续对话示例
现在你的 Agent 可以处理这种带有上下文依赖的逻辑了：

``` java
@Component
public class MemoryDemo implements CommandLineRunner {
    @Autowired private StatefulAgentService agent;

    @Override
    public void run(String... args) {
        String sessionId = "user-123";

        // 第一轮：触发工具调用
        System.out.println("User: 伦敦现在的天气怎么样？");
        System.out.println("AI: " + agent.chat(sessionId, "伦敦现在的天气怎么样？"));
        // 输出：伦敦当前天气多云，气温 15°C。

        // 第二轮：依赖第一轮的记忆（指代“那里”）
        System.out.println("\nUser: 那里的人口是多少？");
        System.out.println("AI: " + agent.chat(sessionId, "那里的人口是多少？"));
        // AI 会理解“那里”指代伦敦，并再次调用工具查询人口。
    }
}
```

## 为什么这个设计更智能？

自动检索上下文：MessageChatMemoryAdvisor 会在请求模型前，自动去 ChatMemory 里检索当前 chatId 下的最近 N 条对话，并把它们拼接在当前 Prompt 之前发送给 LLM。
低代码侵入：你的业务逻辑方法 chat() 依然很简洁，复杂的记忆拉取和存储逻辑都被封装在 Advisor 这个“黑盒子”里了。
持久化支持：如果你想把记忆存入 Redis 或数据库，只需要替换 ChatMemory 的实现类（如使用 CassandraChatMemory 或自定义实现），其他业务代码一行都不用改。

