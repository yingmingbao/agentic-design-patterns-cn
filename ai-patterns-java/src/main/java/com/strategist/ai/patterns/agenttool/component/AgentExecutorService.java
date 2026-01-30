package com.strategist.ai.patterns.agenttool.component;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallback;
import org.springframework.ai.tool.support.ToolDefinitions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.client.ResourceAccessException;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@Service
public class AgentExecutorService {

    private final ChatClient chatClient;

    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;


    @Autowired
    private DashScopeChatModel dashScopeChatModel;

    public AgentExecutorService(ChatClient.Builder builder) {
        // 绑定定义的工具 Bean 名称
        this.chatClient = builder
                .defaultSystem("你是一个有用的助手")
                .build();
    }

    // 异步执行查询，模拟 asyncio.gather
    public CompletableFuture<String> runAgentAsync(String query) {
        return CompletableFuture.supplyAsync(() -> {
            System.out.println("\n--- Agent 运行查询：'" + query + "' ---");

            // 构造 Prompt
            UserMessage userMessage = new UserMessage(query);
            ToolCallback toolCallback = getToolCallback();

            // 调用模型
            try {
                ReactAgent agent = ReactAgent.builder()
                        .name("current-Agent")
                        .model(dashScopeChatModel)
                        .tools(toolCallback).build();
                return agent.call(userMessage).getText();
            } catch (GraphRunnerException e) {
                throw new RuntimeException(e);
            }

        });
    }

    @NotNull
    private static ToolCallback getToolCallback() {
        Method method = ReflectionUtils.findMethod(DateTimeTools.class, "getCurrentDateTime");
        ToolCallback toolCallback = MethodToolCallback.builder()
                .toolDefinition(ToolDefinitions.builder(method)
                        .description("Get the current date and time in the user's timezone")
                        .build())
                .toolMethod(method)
                .toolObject(new DateTimeTools())
                .build();
        return toolCallback;
    }

}
