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



# Spring AI Alibaba 实现：并行调研流水线

## 1. 定义调研工具 (Google Search)
在 Java 中，我们将搜索功能定义为一个可被 AI 调用的 Function。

``` java
@Configuration
public class SearchTools {

    @Bean
    @Description("使用搜索引擎查找最新的能源、交通或气候技术资讯")
    public Function<SearchRequest, String> googleSearch() {
        return request -> {
            // 这里对接真正的搜索 API，如 Serper 或 Google Custom Search
            return "模拟搜索结果：针对 " + request.query() + " 的最新进展...";
        };
    }

    public record SearchRequest(String query) {}
}
```

## 2. 实现核心流水线逻辑

我们将三个调研任务异步启动，并使用 ChatClient 模拟各个 Agent 的指令。

``` java
@Service
public class ResearchPipelineService {

    private final ChatClient chatClient;

    public ResearchPipelineService(ChatClient.Builder builder) {
        // 绑定搜索工具
        this.chatClient = builder.defaultFunctions("googleSearch").build();
    }

    public String runResearchPipeline() {
        // --- 1. 并行调研阶段 (相当于 ParallelAgent) ---
        
        CompletableFuture<String> renewableTask = CompletableFuture.supplyAsync(() -> 
            fetchResearch("调研‘可再生能源最新进展’，简明总结（1-2句）。"));

        CompletableFuture<String> evTask = CompletableFuture.supplyAsync(() -> 
            fetchResearch("调研‘电动汽车技术最新进展’，简明总结（1-2句）。"));

        CompletableFuture<String> carbonTask = CompletableFuture.supplyAsync(() -> 
            fetchResearch("调研‘碳捕集方法现状’，简明总结（1-2句）。"));

        // 等待所有并行任务完成
        CompletableFuture.allOf(renewableTask, evTask, carbonTask).join();

        // --- 2. 整合汇总阶段 (相当于 SynthesisAgent + SequentialAgent) ---
        try {
            return chatClient.prompt()
                .system("""
                    你是一名负责整合调研结果的 AI 助手。
                    请严格基于以下输入摘要生成结构化报告，不得添加外部知识。
                    格式要求：
                    ## 可持续技术最新进展摘要
                    ### 可再生能源发现
                    {renewable}
                    ### 电动汽车发现
                    {ev}
                    ### 碳捕集发现
                    {carbon}
                    ### 总结
                    (简要总结 1-2 句)
                    """)
                .user(u -> u.text("请执行整合任务")
                        .param("renewable", renewableTask.get())
                        .param("ev", evTask.get())
                        .param("carbon", carbonTask.get()))
                .call()
                .content();
        } catch (Exception e) {
            return "整合过程出错: " + e.getMessage();
        }
    }

    private String fetchResearch(String instruction) {
        return chatClient.prompt()
                .system("你是一名专业的调研助手。利用工具搜索并输出摘要。")
                .user(instruction)
                .call()
                .content();
    }
}

```


为什么在 Spring AI 中这样处理更优？
确定性流程控制：ADK 的 SequentialAgent 是一种抽象封装，而 Java 直接编写逻辑流（Step 1 -> Step 2）更符合程序员的直觉，且极易通过 if/else 处理中间异常。

资源利用率：Java 的 CompletableFuture 可以利用系统线程池。如果使用了 Java 21+，你甚至可以使用 虚拟线程 (Virtual Threads) 来同时运行成百上千个并行调研 Agent，而不会撑爆内存。

工具解耦：在 Spring 中，googleSearch 工具是一个 Bean，它可以被任何 Agent 共享，也可以轻松地进行单元测试。


进阶：如何增加实时性？
如果这些调研非常慢，你可能不想让用户干等着。你可以将 CompletableFuture 改为 Flux.merge()，从而实现：

调研员 1 完成 -> 推送到前端

调研员 2 完成 -> 推送到前端

全部完成 -> 最后推送汇总报告

# Spring AI 的 SseEmitter 将这些中间调研结果实时推送到 Web 页面上吗？ 

 实现方案：异步并行 + SSE 推送
## 1. 控制器层 (Controller)
这是与前端对接的入口，负责建立长连接。
``` java
@RestController
@RequestMapping("/api/research")
public class ResearchController {

    private final ResearchPipelineService researchService;

    public ResearchController(ResearchPipelineService researchService) {
        this.researchService = researchService;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamResearch(@RequestParam String topic) {
        SseEmitter emitter = new SseEmitter(120_000L); // 设置2分钟超时
        researchService.runResearchAndPush(topic, emitter);
        return emitter;
    }
}
```
## 2. 业务逻辑层 (Service)
这里是核心：我们同时启动调研，并使用线程池的回调来推送到 SSE 管道。
``` java
@Service
public class ResearchPipelineService {

    private final ChatClient chatClient;

    public ResearchPipelineService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public void runResearchAndPush(String topic, SseEmitter emitter) {
        // 1. 并行启动三个子任务
        CompletableFuture<String> task1 = doResearch("可再生能源", emitter);
        CompletableFuture<String> task2 = doResearch("电动汽车", emitter);
        CompletableFuture<String> task3 = doResearch("碳捕集", emitter);

        // 2. 当所有任务完成后的最终整合
        CompletableFuture.allOf(task1, task2, task3).thenRun(() -> {
            try {
                String summary = synthesize(task1.join(), task2.join(), task3.join());
                emitter.send(SseEmitter.event().name("final").data(summary));
                emitter.complete(); // 全部结束
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });
    }

    private CompletableFuture<String> doResearch(String subject, SseEmitter emitter) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                emitter.send(SseEmitter.event().name("progress").data("正在调研: " + subject));
                
                // 执行 AI 调研逻辑
                String result = chatClient.prompt()
                        .user("针对" + subject + "进行简要调研...")
                        .call().content();

                emitter.send(SseEmitter.event().name("step-complete").data(subject + " 调研完成！"));
                return result;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private String synthesize(String r1, String r2, String r3) {
        // 这里的逻辑与之前相同，将结果汇总
        return chatClient.prompt().user("请整合以下结果...").call().content();
    }
}
```
## 3. 前端如何接收？

前端只需要简单的 JavaScript 即可监听这些来自 Spring AI 的“中间报告”：

``` javascript
const eventSource = new EventSource('/api/research/stream?topic=可持续技术');

eventSource.addEventListener('progress', (e) => {
    console.log("状态更新:", e.data); // 页面显示：正在调研可再生能源...
});

eventSource.addEventListener('step-complete', (e) => {
    console.log("进度通知:", e.data); // 页面显示：可再生能源调研完成！
});

eventSource.addEventListener('final', (e) => {
    document.getElementById('report').innerHTML = e.data; // 显示最终精美的 Markdown 报告
    eventSource.close();
});

```

通过将 Google ADK 的 Agent 思想 与 Spring MVC 的 SSE 机制 结合，你实际上在 Java 环境中构建了一个“可视化智能体流水线”。

