package com.strategist.ai.service.component;


import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.strategist.ai.entity.SysDeepResearch;
import com.strategist.ai.patterns.planning.dto.AgentRequest;
import com.strategist.ai.service.SysDeepResearchService;
import com.strategist.ai.util.UserContext;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class DeepResearchAgentService {

    private final ChatModel chatModel;

    @Resource
    private com.strategist.ai.service.SysAgentChatDialogDetailService sysAgentChatDialogDetailService;

    public DeepResearchAgentService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public String runDeepResearch(String userQuery) throws GraphRunnerException {
        // 1. 构建 ReactAgent
        ReactAgent researcher = ReactAgent.builder()
                .model(chatModel)
                .name("深度研究员")
                .instruction("""
                        你是一名专业研究员。你的目标是撰写结构化、数据驱动的报告。
                        过程要求：
                        1. 先进行深度推理，拆解研究问题。
                        2. 使用搜索工具获取事实依据。
                        3. 在正文中插入引用。
                        """)
//                .functions("webSearch") // 挂载上面定义的搜索工具
                .build();

        // 2. 执行任务 (使用 RC2 标准的 invoke 方法)
        AgentRequest request = new AgentRequest(userQuery);
        Optional<OverAllState> stateOptional = researcher.invoke(request.getQuery());
        return extractDeepResearchReport(stateOptional);
    }

    public Flux<ServerSentEvent<String>> runDeepResearchStream(String userQuery, Long dialogId) {
        // Capture User Context
        Long userId = UserContext.getUserId();
        // Long tenantId = UserContext.getTenantId(); // Removed as SysAgentChatDialogDetail doesn't use it directly or handles it internally?
        // Actually SysAgentChatDialogDetailServiceImpl uses UserContext for createBy/updateBy.
        // It doesn't seem to store tenantId explicitly in entity based on my read of SysAgentChatDialogDetail.
        // Assuming SysAgentChatDialogDetail is not tenant-isolated or tenant is handled by underlying framework/interceptor if needed.
        // But let's check SysDeepResearch usage - it had tenantId.
        // If SysAgentChatDialogDetail doesn't have tenantId, we skip it.
        
        String username = UserContext.getUsername();

        StringBuilder reasoningBuffer = new StringBuilder();
        StringBuilder contentBuffer = new StringBuilder();

        // 1. 构建 Prompt (包含 System Instruction)
        Message systemMessage = new SystemMessage("""
                你是一名专业研究员。你的目标是撰写结构化、数据驱动的报告。
                过程要求：
                1. 先进行深度推理，拆解研究问题。
                2. 使用搜索工具获取事实依据。
                3. 返回思考过程。
                4. 然后返回研究报告正文，在正文中插入引用。
                """);
        Message userMessage = new UserMessage(userQuery);
        Prompt prompt = new Prompt(List.of(systemMessage, userMessage));

        // 2. 流式调用
        return chatModel.stream(prompt)
                .map(chatResponse -> {
                    String reasoning = "";
                    String content = "";

                    if (chatResponse.getResult() != null) {
                        org.springframework.ai.chat.model.Generation generation = chatResponse.getResult();
                        if (generation.getOutput() != null) {
                            AssistantMessage am = generation.getOutput();
                            // Try to extract reasoning from AssistantMessage metadata
                            if (am.getMetadata() != null) {
                                Object r = am.getMetadata().get("reasoningContent");
                                if (r == null) {
                                    r = am.getMetadata().get("reasoning"); // Check alternative key
                                }
                                if (r != null) {
                                    reasoning = r.toString();
                                }
                            }
                            // Try to extract from Generation metadata if not found
                            if (!StringUtils.hasText(reasoning) && generation.getMetadata() != null) {
                                Object r = generation.getMetadata().get("reasoningContent");
                                if (r == null) {
                                    r = generation.getMetadata().get("reasoning");
                                }
                                if (r != null) {
                                    reasoning = r.toString();
                                }
                            }
                            content = am.getText();
                        }
                    }

                    // Accumulate content
                    if (StringUtils.hasText(reasoning)) {
                        reasoningBuffer.append(reasoning);
                        return ServerSentEvent.<String>builder()
                                .event("thinking")
                                .data(reasoning)
                                .build();
                    } else if (StringUtils.hasText(content)) {
                        contentBuffer.append(content);
                        return ServerSentEvent.<String>builder()
                                .event("content")
                                .data(content)
                                .build();
                    }
                    return ServerSentEvent.<String>builder().comment("keep-alive").build();
                })
                .doOnComplete(() -> {
                    // Save to DB when stream completes
                    try {
                        // Restore context if needed (UserContext is ThreadLocal, might be lost in reactive chain completion)
                        // But we captured values above.
                        // Wait, SysAgentChatDialogDetailServiceImpl uses UserContext.getUsername() internally!
                        // We must set it back.
                        UserContext.setUserId(userId);
                        UserContext.setUsername(username);
                        
                        sysAgentChatDialogDetailService.saveMessage(
                            dialogId,
                            "assistant",
                            contentBuffer.toString(),
                            "deepsearch", // source/model
                            "deepsearch", // type
                            reasoningBuffer.toString()
                        );
                    } catch (Exception e) {
                        e.printStackTrace(); // Log error but don't fail the stream
                    } finally {
                        UserContext.clear();
                    }
                })
                .filter(sse -> sse.data() != null || sse.comment() != null);
    }
    public String extractDeepResearchReport(Optional<OverAllState> stateOptional) {
        if (stateOptional.isPresent()) {
            OverAllState finalState = stateOptional.get();

            // 1. 直接获取 data 这个 Map (截图显示 data 是 HashMap)
            Map<String, Object> dataMap = finalState.data();

            if (dataMap != null && dataMap.containsKey("messages")) {
                // 2. 获取消息列表
                List<Message> messages = (List<Message>) dataMap.get("messages");

                if (!messages.isEmpty()) {
                    // 3. 拿到最后一条消息（即 DeepSeek R1 生成的最终报告）
                    Message lastMessage = messages.get(messages.size() - 1);

                    // 如果使用的是 deepseek-reasoner，可以尝试获取思考过程
                    if (lastMessage instanceof AssistantMessage assistantMsg) {
                        // 打印思考过程（DeepSeek 扩展字段）
                        String reasoning = (String) assistantMsg.getMetadata().get("reasoning_content");
                        if (reasoning != null) {
                            System.out.println("=== 思考过程 ===\n" + reasoning);
                        }
                    }

                    return lastMessage.getText();
                }
            }
        }
        return "未能解析到报告内容";
    }

}
