package com.strategist.ai.patterns.routing.component;

import com.strategist.ai.patterns.routing.SubAgentHandler;
import com.strategist.ai.patterns.routing.component.dto.BookingDetail;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component("booker")
public class BookingHandler implements SubAgentHandler {

    private final ChatClient chatClient;

    public BookingHandler(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public String handle(String request) {
        System.out.println("\n--- 正在执行结构化预订提取 ---");

        // 核心逻辑：将输入解析为 BookingDetail 对象
        BookingDetail detail = chatClient.prompt()
                .system("你是一个专业的预订助手。请从用户的请求中提取预订细节。 ")
                .user(request)
                .call()
                .entity(BookingDetail.class); // 自动解析并填充对象

        // 模拟业务逻辑处理
        assert detail != null;
        return String.format("请求：" + request + "【预订成功】类型：%s, 目的地：%s, 日期：%s, 备注：%s",
                detail.getType(), detail.getDestination(), detail.getDate(), detail.getSpecialRequests());
    }

}
