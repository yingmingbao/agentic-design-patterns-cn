package com.strategist.ai.patterns.planning;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.SequentialAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.store.Store;
import com.alibaba.cloud.ai.graph.store.StoreItem;
import com.strategist.ai.patterns.planning.dto.AgentRequest;
import com.strategist.ai.patterns.planning.dto.AgentResponse;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ArticleCrewPlanningService {

    private final ChatModel chatModel;

    public ArticleCrewPlanningService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public String runArticleTask(String topic) throws GraphRunnerException {
        // 1. 定义智能体 (对应 CrewAI 的 Agent)
        // Spring AI Alibaba 通过 instruction 注入角色、目标和背景
//        LlmAgent plannerWriterAgent = LlmAgent.builder()
        ReactAgent plannerWriterAgent = ReactAgent.builder()
                .model(chatModel)
                .name("文章规划与写作专家")
                .description("负责规划并撰写技术文章摘要")
                .instruction("""
                        角色：你是一名资深技术写手和内容策略师。
                        目标：规划并撰写指定主题的简明、吸引人的摘要。
                        背景：你的优势在于写作前先制定清晰可执行的计划，确保最终摘要既信息丰富又易于理解。
                        
                        任务要求：
                        1. 针对用户提供的主题，先制定摘要的要点计划（项目符号列表）。
                        2. 根据计划撰写约 200 字的摘要。
                        
                        最终报告格式：
                        ### 计划
                        - [要点列表]
                        
                        ### 摘要
                        - [结构化总结内容]
                        """)
                .build();

        // 2. 编排任务 (对应 CrewAI 的 Crew + Process.sequential)
        // 在 Spring AI Alibaba 中，SequentialAgent 用于按顺序执行子智能体
        // 这里虽然只有一个 Agent，但可以使用此模式保持结构对等
        // 2. 编排流水线
        SequentialAgent articleCrew = SequentialAgent.builder()
                .name("Article_Pipeline")
                .subAgents(List.of(plannerWriterAgent))
                // 关键：明确指定最终结果存放在 "final_result" 键中
                .build();
        // 3. 执行任务
        System.out.println("## 正在运行规划与写作任务 ##");
        //  执行
        Optional<OverAllState> stateOptional = articleCrew.invoke(new AgentRequest("主题：" + topic).getQuery());
        if (stateOptional.isPresent()) {
            // 获取 OverAllState 对象
            OverAllState finalState = stateOptional.get();

            // 1. 获取 data 这个 HashMap
            Map<String, Object> data = finalState.data();

            // 2. 从 data 中获取 messages 列表
            List<Object> messages = (List<Object>) data.get("messages");

            if (messages != null && !messages.isEmpty()) {
                // 3. 获取最后一条消息（AI 的回答）
                Object lastMessage = messages.get(messages.size() - 1);

                // 截图显示这是个对象，通常可以通过反射或 toString 获取文本
                // 在 Spring AI 中它通常是 AssistantMessage 类型
                System.out.println("最终生成内容: " + lastMessage);
                return ((AssistantMessage) lastMessage).getText();
            }
        }
        return "Agent 执行失败或未返回任何状态";
    }
}
