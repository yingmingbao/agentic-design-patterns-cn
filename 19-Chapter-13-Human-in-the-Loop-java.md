### 复杂的 Agent 逻辑与**拦截器（Callback）**功能，可以利用 ChatClient 的 Advisor（顾问）机制来替代 Python 中的 CallbackContext。Advisor 允许在请求发送给大模型之前动态修改提示词。

## 1. 定义工具 (Tools)
首先，将 Python 函数转换为 Spring AI 的 @Tool 组件。

``` java
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class SupportTools {

    @Tool(description = "分析并提供故障排查步骤")
    public Map<String, String> troubleshootIssue(String issue) {
        return Map.of("status", "success", "report", "排查步骤：" + issue);
    }

    @Tool(description = "为未解决的问题创建工单")
    public Map<String, String> createTicket(String issueType, String details) {
        return Map.of("status", "success", "ticket_id", "TICKET123");
    }

    @Tool(description = "将复杂问题升级给人工专家")
    public Map<String, String> escalateToHuman(String issueType) {
        return Map.of("status", "success", "message", issueType + " 已升级至专家队列。");
    }
}
```

## 2. 实现个性化拦截器 (Advisor)
Spring AI 的 Advisor 是实现 Python CallbackContext 逻辑的最佳位置。它可以在运行时拦截上下文并注入用户信息。

``` java
import org.springframework.ai.chat.client.ChatClientCustomizer;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.AdvisedRequest;
import org.springframework.ai.chat.client.advisor.api.AdvisedResponse;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

public class PersonalizationAdvisor implements CallAroundAdvisor {

    private final Map<String, Object> customerState;

    public PersonalizationAdvisor(Map<String, Object> customerState) {
        this.customerState = customerState;
    }

    @Override
    public AdvisedResponse aroundCall(AdvisedRequest advisedRequest, CallAroundAdvisorChain chain) {
        // 模拟 Python 中的 personalization_callback 逻辑
        Map<String, Object> info = (Map<String, Object>) customerState.get("customer_info");
        
        String note = String.format("""
                
                重要个性化信息：
                客户姓名：%s
                客户等级：%s
                最近购买：%s
                """, 
                info.getOrDefault("name", "尊贵客户"),
                info.getOrDefault("tier", "标准"),
                info.getOrDefault("recent_purchases", "无")
        );

        // 将个性化信息注入到 User Message 之前或作为 System Prompt 补充
        AdvisedRequest modifiedRequest = AdvisedRequest.from(advisedRequest)
                .userText(advisedRequest.userText() + note)
                .build();

        return chain.nextAroundCall(modifiedRequest);
    }

    @Override
    public int getOrder() { return 0; }
}
```

## 3. 构建 Agent 实例 (ChatClient)

使用 ChatClient 组合指令、工具和拦截器。

``` java
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@Configuration
public class TechnicalSupportConfig {

    @Bean
    public ChatClient technicalSupportAgent(ChatClient.Builder builder, SupportTools tools) {
        // 模拟初始 State
        Map<String, Object> state = Map.of(
            "customer_info", Map.of(
                "name", "张三",
                "tier", "金卡",
                "recent_purchases", List.of("智能手机", "蓝牙耳机"),
                "support_history", "曾咨询电池续航问题"
            )
        );

        String systemInstruction = """
                你是一家电子产品公司的技术支持专家。
                首先，检查用户的支持历史。如有，请在回复中引用。
                历史信息：{history}
                
                技术问题处理流程：
                1. 使用 troubleshootIssue 工具分析。
                2. 指导基础排查。
                3. 未解决则调用 createTicket。
                复杂问题直接调用 escalateToHuman。
                保持专业同理心。
                """;

        return builder
                .defaultSystem(s -> s.text(systemInstruction)
                                     .param("history", ((Map)state.get("customer_info")).get("support_history")))
                .defaultTools(tools)
                .defaultAdvisors(new PersonalizationAdvisor(state))
                .build();
    }
}
```

### 如何执行
在 Service 中直接调用：

String response = chatClient.prompt("我的手机屏幕无法亮起").call().content();

这种架构支持横向扩展。如果你需要接入向量数据库（RAG）来增强历史记录检索，只需要在 defaultAdvisors 中添加一个 QuestionAnswerAdvisor 即可。

