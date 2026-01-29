在 Spring AI Alibaba 1.1.0-RC2 框架中，实现这种“主处理器 + 失败回退 + 结果呈现”的顺序逻辑，主要依靠 SequentialAgent 结合共享的 OverAllState（状态字典）来完成。

与 Python ADK 略有不同的是，Spring AI Alibaba 的 Agent 默认会自动将工具调用的结果同步到共享状态中，你可以通过自定义 Advisor 或在 instruction 中直接引用 state 键值。

## 1. 定义工具 (Tools)

在 Java 中，我们使用 @Tool 注解来定义工具。

``` Java
@Component
public class LocationTools {

    @Tool(description = "获取精确的地理位置信息")
    public String getPreciseLocationInfo(@ToolParam(description = "街道地址") String address) {
        // 模拟逻辑：如果包含 'failed'，则模拟失败
        if (address.contains("failed")) {
            return "ERROR: Precise lookup failed";
        }
        return "精确坐标: 39.9042, 116.4074 (北京)";
    }

    @Tool(description = "获取城市的通用区域信息")
    public String getGeneralAreaInfo(@ToolParam(description = "城市名称") String city) {
        return "通用信息: 这是 " + city + " 的中心区域";
    }
}
```

## 2. Java 实现：顺序回退智能体流水线
``` Java
@Service
public class RobustLocationService {

    private final ChatModel chatModel;

    public RobustLocationService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public void runRobustLocationAgent(String userQuery) {
        // Agent 1: 主处理器
        ReactAgent primaryHandler = ReactAgent.builder()
                .chatModel(chatModel)
                .name("primary_handler")
                .instruction("使用 get_precise_location_info 工具查询地址。如果失败，请明确回复 'LOOKUP_FAILED'。")
                .functions("getPreciseLocationInfo")
                .outputKey("location_result") // 结果存入状态字典
                .build();

        // Agent 2: 失败回退处理器 (具有逻辑判断)
        ReactAgent fallbackHandler = ReactAgent.builder()
                .chatModel(chatModel)
                .name("fallback_handler")
                .instruction("""
                    检查状态中的 location_result。
                    如果包含 'LOOKUP_FAILED' 或 'ERROR'，则从用户输入中提取城市名，
                    并调用 get_general_area_info 工具。
                    否则，直接跳过，不做任何操作。
                    """)
                .functions("getGeneralAreaInfo")
                .outputKey("location_result") // 覆盖或更新结果
                .build();

        // Agent 3: 结果呈现器
        ReactAgent responseAgent = ReactAgent.builder()
                .chatModel(chatModel)
                .name("response_agent")
                .instruction("查看 location_result 的内容。向用户清晰地展示最终位置信息。如果没有信息，请致歉。")
                .build();

        // 核心：使用 SequentialAgent 保证执行顺序
        SequentialAgent robustLocationAgent = SequentialAgent.builder()
                .name("robust_location_agent")
                .subAgents(List.of(primaryHandler, fallbackHandler, responseAgent))
                .build();

        // 执行任务
        AgentRequest request = new AgentRequest(userQuery);
        Optional<OverAllState> stateOptional = robustLocationAgent.invoke(request);

        // 打印最终结果
        stateOptional.ifPresent(state -> {
            System.out.println("最终输出: " + state.getLatestMessage().getContent());
            System.out.println("状态快照: " + state.data());
        });
    }
}
```

## 3. 关键机制说明
状态共享 (OverAllState)：在 SequentialAgent 中，所有的子 Agent 共享同一个状态容器。Agent 1 通过 outputKey("location_result") 存入的值，Agent 2 可以通过 instruction 指示模型去读取。

错误处理逻辑：Python 代码中通过 state["primary_location_failed"] = True 进行显式标记。在 Spring AI Alibaba 中，你可以：

语义判断：如上例，让 LLM 根据前一个步骤的内容（"LOOKUP_FAILED"）自行判断。

程序判断 (推荐)：使用 RoutingAgent 替代 SequentialAgent。RoutingAgent 允许你编写 Java 代码片段来决定下一个路由，这比让 LLM 去读 state 更加精准。

## 4. 进阶：使用 RoutingAgent 进行精准回退
如果你希望回退逻辑更稳健（不依赖 LLM 的判断力），可以这样写：

``` java
RoutingAgent robustRouter = RoutingAgent.builder()
        .chatModel(chatModel)
        .route(state -> {
            String result = (String) state.data().get("location_result");
            if (result == null || result.contains("failed")) {
                return "fallback_handler"; // 路由到回退 Agent
            }
            return "response_agent"; // 直接跳到响应 Agent
        })
        .build();

```
