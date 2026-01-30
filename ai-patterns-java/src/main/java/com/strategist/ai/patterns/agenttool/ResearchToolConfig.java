package com.strategist.ai.patterns.agenttool;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.function.Function;

@Configuration
public class ResearchToolConfig {

    @Bean
    @Description("搜索互联网以获取关于医疗体系、药物经济学和司美格鲁肽的最新数据")
    public Function<SearchRequest, String> webSearch() {
        return request -> {
            // 这里对接你的搜索 API (如 Serper, Google Search 等)
            return "模拟搜索结果：司美格鲁肽在2024年降低了相关国家15%的肥胖并发症支出...";
        };
    }

    public record SearchRequest(String query) {}
}
