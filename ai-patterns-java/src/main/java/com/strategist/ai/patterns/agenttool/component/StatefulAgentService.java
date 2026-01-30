package com.strategist.ai.patterns.agenttool.component;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallback;
import org.springframework.ai.tool.support.ToolDefinitions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.client.ResourceAccessException;

import java.lang.reflect.Method;
import java.util.ArrayList;

@Service
public class StatefulAgentService {

    @Autowired
    private DashScopeChatModel dashScopeChatModel;

    public String chat(String chatId, String userQuery) throws GraphRunnerException {

        ToolCallback toolCallback = getToolCallback();
        ReactAgent agent = ReactAgent.builder()
                .model(dashScopeChatModel)
                .name("my_agent")
                .tools(toolCallback)
                .saver(new MemorySaver())
                .build();

        // 使用 thread_id 维护对话上下文
        RunnableConfig config = RunnableConfig.builder()
                .threadId(chatId) // threadId 指定会话 ID
                .build();

        try {
            String message = agent.call(userQuery).getText();
            System.out.println(message);
            return message;
        } catch (ResourceAccessException e) {
            System.err.println("API 请求超时或网络错误: " + e.getMessage());
            throw new GraphRunnerException("API 请求失败: " + e.getMessage(), e);
        }
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
