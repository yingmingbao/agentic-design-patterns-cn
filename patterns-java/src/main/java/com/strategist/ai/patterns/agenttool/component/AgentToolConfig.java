package com.strategist.ai.patterns.agenttool.component;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;
import org.springframework.ai.chat.client.ChatClient;

import java.util.function.Function;


@Configuration(proxyBeanMethods = false)
public class AgentToolConfig {

    public static final String CURRENT_WEATHER_TOOL = "currentWeather";

    @Bean(CURRENT_WEATHER_TOOL)
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

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }
}
