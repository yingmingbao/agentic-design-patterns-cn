
在 Spring AI Alibaba（以及 Spring AI 核心）中，实现这种“路由分发”模式不需要 Python 里的 RunnableBranch 这种复杂抽象。Java 的强类型特性和 Spring 的 Bean 管理让逻辑更加清晰。

# Spring AI Alibaba 的标准实现方式

## 配置文件 (application.yml)
首先，将 API 配置从代码中抽离，这是 Spring 生态的标准做法。

``` yaml
spring:
  ai:
    openai:
      api-key: ${DEEPSEEK_API_KEY} # 建议使用环境变量，别硬编码 Key
      base-url: https://api.deepseek.com/v1
      chat:
        options:
          model: deepseek-chat
          temperature: 0
```

## 2. 编写处理器（Handlers）
  使用策略模式，将不同的处理逻辑封装为独立的 Bean。
``` java
import org.springframework.stereotype.Component;

public interface SubAgentHandler {
    String handle(String request);
}

@Component("booker")
class BookingHandler implements SubAgentHandler {
    public String handle(String request) {
        System.out.println("\n--- 委托给预订处理器 ---");
        return "预订处理器已处理请求：'" + request + "'。结果：模拟预订动作。";
    }
}

@Component("info")
class InfoHandler implements SubAgentHandler {
    public String handle(String request) {
        System.out.println("\n--- 委托给信息处理器 ---");
        return "信息处理器已处理请求：'" + request + "'。结果：模拟信息检索。";
    }
}

@Component("unclear")
class UnclearHandler implements SubAgentHandler {
    public String handle(String request) {
        System.out.println("\n--- 处理不明确请求 ---");
        return "协调者无法委托请求：'" + request + "'。请补充说明。";
    }
}
```

## 3. 构建协调者智能体（Coordinator Agent）

我们使用 ChatClient 进行意图判断，并利用 Java 的 Map 自动注入实现路由。

``` java
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class CoordinatorAgent {

    private final ChatClient chatClient;
    private final Map<String, SubAgentHandler> handlers;

    // Spring 会自动将所有 SubAgentHandler 的实现类注入到这个 Map 中
    public CoordinatorAgent(ChatClient.Builder builder, Map<String, SubAgentHandler> handlers) {
        this.chatClient = builder.build();
        this.handlers = handlers;
    }

    public String execute(String userRequest) {
        // 1. 路由决策 (等同于 coordinator_router_chain)
        String decision = chatClient.prompt()
                .system("""
                    分析用户请求，判断应由哪个专属处理器处理。
                    - 若请求涉及预订机票或酒店，输出 'booker'。
                    - 其他一般信息问题，输出 'info'。
                    - 若请求不明确或不属于上述类别，输出 'unclear'。
                    只输出一个词：'booker'、'info' 或 'unclear'。
                    """)
                .user(userRequest)
                .call()
                .content()
                .trim()
                .toLowerCase();

        // 2. 委托执行 (等同于 RunnableBranch / delegation_branch)
        // 如果决策结果不在 Map 中，默认走 unclear 分支
        SubAgentHandler handler = handlers.getOrDefault(decision, handlers.get("unclear"));
        
        return handler.handle(userRequest);
    }
}


```
## 4. 示例用法（Main 执行）
在 Spring Boot 应用中，你可以直接在 CommandLineRunner 或 Controller 中调用。

``` java
@SpringBootApplication
public class AgentApplication implements CommandLineRunner {

    @Autowired
    private CoordinatorAgent coordinator;

    public static void main(String[] args) {
        SpringApplication.run(AgentApplication.class, args);
    }

    @Override
    public void run(String... args) {
        System.out.println("--- 预订请求示例 ---");
        System.out.println("最终结果 A: " + coordinator.execute("帮我预订飞往伦敦的机票。"));

        System.out.println("\n--- 信息请求示例 ---");
        System.out.println("最终结果 B: " + coordinator.execute("意大利的首都是哪里？"));

        System.out.println("\n--- 不明确请求示例 ---");
        System.out.println("最终结果 C: " + coordinator.execute("讲讲量子物理。"));
    }
}
```
为什么在 Spring AI 里这么写？
类型安全：避免了 Python 字典中复杂的 x['request']['request'] 这种嵌套取值，Java 的对象引用让数据流一目了然。

解耦：如果你想增加一个 weather 处理器，只需要增加一个类实现 SubAgentHandler，不需要修改 CoordinatorAgent 的任何代码。

鲁棒性：Spring 的 ChatClient 内置了对 DeepSeek 等 OpenAI 兼容接口的支持，不会因为 Python 版本升级导致的属性名（如 model vs model_name）变化而崩溃。

## 改进编码

利用 Spring AI 的 Structured Output，我们可以强制要求模型返回一个预定义的 Java Enum。

第一步：定义路由枚举类
首先，创建一个强类型的枚举。这不仅是为了路由，还能让 IDE 提供自动补全。

``` java
public enum AgentType {
    BOOKER,   // 预订处理器
    INFO,     // 信息处理器
    UNCLEAR   // 无法处理
}
```
第二步：使用 entity() 重构协调者逻辑
Spring AI 的 ChatClient 能够自动解析模型返回的 JSON 并将其映射到 Java 类型（包括枚举）。

``` java
@Service
public class CoordinatorAgent {

    private final ChatClient chatClient;
    private final Map<String, SubAgentHandler> handlers;

    public CoordinatorAgent(ChatClient.Builder builder, Map<String, SubAgentHandler> handlers) {
        this.chatClient = builder.build();
        this.handlers = handlers;
    }

    public String execute(String userRequest) {
        // 1. 结构化路由决策
        // Spring AI 会自动在 Prompt 后添加格式化指令，并解析返回结果
        AgentType decision;
        try {
               decision = chatClient.prompt()
                .system("""
                    分析用户请求，判断应由哪个专属处理器处理。
                    分类标准：
                    - BOOKER: 涉及预订机票、酒店或行程。
                    - INFO: 一般性的事实查询、百科知识。
                    - UNCLEAR: 请求不明确、超出上述范围或无法处理。
                    """)
                .user(userRequest)
                .call()
                .entity(AgentType.class); // 核心变化：直接获取枚举对象
        } catch (Exception e){

          decision = AgentType.UNCLEAR; // 解析失败时的兜底策略
        }

        System.out.println("系统智能决策结果: " + decision);

        // 2. 更加安全的委托执行
        // 将枚举转为小写字符串去 Map 中匹配 Bean
        String handlerKey = decision.name().toLowerCase();
        SubAgentHandler handler = handlers.getOrDefault(handlerKey, handlers.get("unclear"));
        
        return handler.handle(userRequest);
    }
}
```

## 让子智能体（如 Booker）也返回一个结构化的 Java 对象（比如 BookingDetail 对象），而不仅仅是返回一段字符串？

1. 定义结构化数据模型 (POJO)
首先，定义一个 Java 类（或 Record）来承载预订信息。我们可以利用 Bean Validation 注解（如 @JsonPropertyDescription）来帮助 AI 更好地理解字段含义。

``` java
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public class BookingDetail {

    @JsonPropertyDescription("目的地城市，例如：伦敦")
    String destination;

    @JsonPropertyDescription("出发日期，格式：YYYY-MM-DD")
    String date;

    @JsonPropertyDescription("预订类型：FLIGHT（机票）或 HOTEL（酒店）")
    String type;

    @JsonPropertyDescription("用户的额外要求，如：靠窗、含早餐")
    String specialRequests;
}
```

2. 让子智能体（Booker）返回对象
现在，我们重写 BookingHandler。它不再返回简单的 String，而是利用 ChatClient 将用户的请求“实体化”。

``` java
@Component("booker")
class BookingHandler implements SubAgentHandler {
    
    private final ChatClient chatClient;

    public BookingHandler(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @Override
    public String handle(String request) {
        System.out.println("\n--- 正在执行结构化预订提取 ---");

        // 核心逻辑：将输入解析为 BookingDetail 对象
        BookingDetail detail = chatClient.prompt()
                .system("你是一个专业的预订助手。请从用户的请求中提取预订细节。")
                .user(request)
                .call()
                .entity(BookingDetail.class); // 自动解析并填充对象

        // 模拟业务逻辑处理
        return String.format("【预订成功】类型：%s, 目的地：%s, 日期：%s, 备注：%s",
                detail.type(), detail.destination(), detail.date(), detail.specialRequests());
    }
}
```

3. 系统整体流转：从路由到结构化提取

现在的整个流程如下：

Coordinator：判断意图，结果为 AgentType.BOOKER。

Booker：接到任务，调用模型将“帮我订一张明天去上海的机票，要靠窗”解析为：

destination: "上海"

date: "202x-xx-xx" (AI 会根据当前日期计算)

type: "FLIGHT"

specialRequests: "靠窗"

最终输出：后端直接拿到了干净的 Java 对象，可以一键存入 MySQL。


4. 关键点：为什么这比 Python 的 LangChain 更好用？
类型安全：在 Java 中，如果你访问 detail.destination()，编译器能保证这个字段一定存在。在 Python 中，你可能在写 result['destinaton']（少了个 i）时才发现报错。

自动 Schema 生成：Spring AI 会自动根据你的 Java 类结构生成 JSON Schema 并发送给 DeepSeek，不需要你手写复杂的 Prompt 来规定格式。

无缝集成：提取出来的 BookingDetail 可以直接配合 Spring Data JPA 保存到数据库，实现从 AI 到业务系统的闭环。


