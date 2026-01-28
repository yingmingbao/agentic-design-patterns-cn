# RunnableParallel 同时启动三个任务（总结、提问、提取关键词）
在 Spring AI Alibaba 中，这种模式可以通过 Java 的 CompletableFuture 或 Project Reactor (Mono.zip) 完美实现。考虑到 Spring AI 深度集成 Reactor，使用非阻塞的并行流是最高效的选择。

Spring AI Alibaba 实现：并行流水线模式
## 1. 定义处理服务
我们将三个子任务封装为独立的方法，并使用 Flux 或 CompletableFuture 实现并行。

``` java
@Service
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
}
```

## 2. 核心对比：为什么 Java 这样写？
 | 概念	| LangChain LCEL (Python)	| Spring AI Alibaba (Java) |
 |	并行声明	 |	RunnableParallel({...}) |	CompletableFuture.supplyAsync()|
 |	数据传递	 |	自动在 dict 中流转 |		通过变量显式注入 Prompt |
 |	同步/异步	 |	ainvoke (需要手动管理 EventLoop)	 |	原生支持多线程/虚拟线程 (Virtual Threads) |
 |	容错处理 |		Runnable.with_fallbacks()	 |	Java try-catch 或 .exceptionally() |

 ## 3. 进阶：响应式流实现 (推荐用于高并发场景)

 ``` java
public Mono<String> runReactiveChain(String topic) {
    // 启动三个非阻塞流
    Mono<String> summary = Mono.fromSupplier(() -> chatClient.prompt().system("总结...").user(topic).call().content()).subscribeOn(Schedulers.boundedElastic());
    Mono<String> questions = Mono.fromSupplier(() -> chatClient.prompt().system("提问...").user(topic).call().content()).subscribeOn(Schedulers.boundedElastic());
    Mono<String> terms = Mono.fromSupplier(() -> chatClient.prompt().system("提取...").user(topic).call().content()).subscribeOn(Schedulers.boundedElastic());

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
```
总结建议
性能优势：在 Java 中，这种并行模式利用了多核 CPU 的优势。对于 DeepSeek 等响应时间较长的模型，三个子任务并行运行将使总耗时从 T1+T2+T3 降低到 Max(T1, T2, T3) + 汇总耗时。

安全性：使用 String.format 或 PromptTemplate 构建最终汇总 Prompt，比 Python 的字典注入更加直观且不容易出错。

调试：在 Java 中你可以轻松地在 summaryFuture 那里打断点，查看中间结果，而 LCEL 的链式结构有时在调试时像个黑盒。
