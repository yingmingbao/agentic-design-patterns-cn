### Chapter-12-Exception-Handling-and-Recovery-java

## 这是因为 Spring AI 的设计哲学更倾向于通过 ChatClient 链式调用或使用 Spring AI Alibaba Agent Framework 中的 AgentExecutor 来实现任务编排。

要实现这种“主处理器 -> 备用处理器 -> 响应生成”的顺序逻辑，最优雅的方式是将逻辑封装在一个 Service 中，利用上下文状态（State）在不同阶段传递信息。

## 1. 定义工具 (Tools)
首先，将 Python 中的工具函数转换为 Java 中的 @Tool 方法。
``` java
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class LocationTools {

    @Tool(description = "获取精确的位置详细信息")
    public String getPreciseLocationInfo(String address) {
        // 模拟精确查找逻辑
        return null; // 假设查找失败返回null
    }

    @Tool(description = "获取城市大致区域信息")
    public String getGeneralAreaInfo(String city) {
        return "城市：" + city + " 的通用区域信息...";
    }
}
```


## 2. 实现顺序 Agent 逻辑
在 Java 中，我们通过一个“编排器” Service 来驱动这三个逻辑阶段，并共享一个 Map 作为 state。

``` java
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

@Service
public class RobustLocationAgent {

    private final ChatClient chatClient;
    private final LocationTools locationTools;

    public RobustLocationAgent(ChatClient.Builder builder, LocationTools locationTools) {
        this.chatClient = builder.defaultTools(locationTools).build();
        this.locationTools = locationTools;
    }

    public String execute(String userAddress) {
        // 模拟全局状态 state
        Map<String, Object> state = new HashMap<>();

        // --- 阶段 1: Primary Handler ---
        String primaryPrompt = "你的任务是获取精确的位置信息。请调用 getPreciseLocationInfo，地址是：" + userAddress;
        String primaryResult = chatClient.prompt(primaryPrompt).call().content();
        
        // 更新状态
        boolean failed = (primaryResult == null || primaryResult.contains("无法获取") || primaryResult.isEmpty());
        state.put("primary_location_failed", failed);
        state.put("location_result", failed ? null : primaryResult);

        // --- 阶段 2: Fallback Handler ---
        if ((Boolean) state.get("primary_location_failed")) {
            String fallbackPrompt = String.format(
                "主处理器失败。请从地址 '%s' 中提取城市名，并调用 getGeneralAreaInfo。", userAddress);
            String fallbackResult = chatClient.prompt(fallbackPrompt).call().content();
            state.put("location_result", fallbackResult);
        }

        // --- 阶段 3: Response Agent ---
        String resultData = (String) state.get("location_result");
        String finalPrompt = String.format(
            "查看位置信息：'%s'。请清晰简明地向用户展示。若信息为空，请致歉并说明无法获取。", 
            resultData == null ? "" : resultData);

        return chatClient.prompt(finalPrompt).call().content();
    }
}
```


## 关键改进建议：
自动提取：在 Python 版中，提取城市名是靠 LLM 识别的。在 Java 版中，我也保留了这一特性，通过阶段 2 的 fallbackPrompt 让模型完成提取并调用工具。

错误处理：Java 的强类型允许你对 state 进行更严格的校验（例如使用自定义的 LocationState 对象），防止 Python 中常见的 Key 错误。
