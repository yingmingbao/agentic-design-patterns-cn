package com.strategist.ai.patterns.parallelization;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.CompletableFuture;

@Component
public class ParallelChainService {

    private final ChatClient chatClient;

    public ParallelChainService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public String runFullParallelChain(String topic) {
        // 1. 定义三个并行任务 (CompletableFuture 方式)
        CompletableFuture<String> summaryFuture = CompletableFuture.supplyAsync(() ->
                chatClient.prompt().system("请简明扼要地总结以下主题：").user(topic).call().content());

        CompletableFuture<String> questionsFuture = CompletableFuture.supplyAsync(() ->
                chatClient.prompt().system("请针对以下主题生成三个有趣的问题：").user(topic).call().content());

        CompletableFuture<String> termsFuture = CompletableFuture.supplyAsync(() ->
                chatClient.prompt().system("请从以下主题中提取 5-10 个关键词，用逗号分隔：").user(topic).call().content());

        // 2. 等待所有并行任务完成 (相当于 RunnableParallel)
        CompletableFuture.allOf(summaryFuture, questionsFuture, termsFuture).join();

        try {
            // 3. 结果汇总并提交给最终模型 (相当于 synthesis_prompt)
            return chatClient.prompt()
                    .system(String.format("""
                                    根据以下信息：
                                    摘要：%s
                                    相关问题：%s
                                    关键词：%s
                                    请综合生成完整答案。
                                    """,
                            summaryFuture.get(),
                            questionsFuture.get(),
                            termsFuture.get()))
                    .user("原始主题：" + topic)
                    .call()
                    .content();
        } catch (Exception e) {
            throw new RuntimeException("汇总链执行失败", e);
        }
    }

    public Mono<String> runReactiveChain(String topic) {
        // 启动三个非阻塞流
        Mono<String> summary = Mono.fromSupplier(
                        () -> chatClient.prompt()
                                .system("总结...")
                                .user(topic)
                                .call()
                                .content())
                .subscribeOn(Schedulers.boundedElastic());
        Mono<String> questions = Mono.fromSupplier(
                        () -> chatClient
                                .prompt()
                                .system("提问...")
                                .user(topic).call()
                                .content())
                .subscribeOn(Schedulers.boundedElastic());
        Mono<String> terms = Mono.fromSupplier(
                        () -> chatClient
                                .prompt().system("提取...")
                                .user(topic).call()
                                .content())
                .subscribeOn(Schedulers.boundedElastic());

        // 压缩 (Zip) 并行结果
        return Mono.zip(summary, questions, terms)
                .flatMap(tuple -> chatClient.prompt()
                        .system("汇总：" + tuple.getT1() + tuple.getT2() + tuple.getT3())
                        .user(topic)
                        .stream() // 支持流式返回
                        .content()
                        .collectList()
                        .map(list -> String.join("", list)));
    }
}
