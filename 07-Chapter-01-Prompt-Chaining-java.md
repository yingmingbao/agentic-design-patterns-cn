
在 Spring AI Alibaba 的体系中，你提到的“流水线模式”通常被称为 Prompt Chaining。它将一个复杂的、容易让 LLM 产生幻觉的大任务，拆解为多个确定性更高的微任务。

虽然 Spring AI 没有 Python 版 LangChain 那种 | 运算符，但它通过 ChatClient 的流式 API 和 Java 函数式编程 提供了更严谨的实现方式。

模式实现：任务拆解流水线
假设我们要实现你提到的两步走任务：提取 (Extraction) -> 转换 (Transformation)。

1. 核心架构设计
在 Spring AI 中，推荐将每一步封装为独立的逻辑单元，利用 Java 的类型安全来保证数据流转。

``` java
@Service
public class PromptPipelineService {

    private final ChatClient chatClient;

    public AiPipelineService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public String executePipeline(String userInput) {
        // 第一阶段：提取关键规格 (Focus: Precision)
        String specs = extractStep(userInput);
        // 第二阶段：转换成最终格式 (Focus: Creativity/Format)
        return transformStep(specs);
    }

    private String extractStep(String input) {
        return chatClient.prompt()
                .user(u -> u.text("从以下内容中提取技术规格参数：{input}")
                .param("input", input))
                .call()
                .content();
    }

    private String transformStep(String specs) {
        return chatClient.prompt()
                .user(u -> u.text("将以下规格参数转换为更易读的对比表格：{specs}")
                .param("specs", specs))
                .call()
                .content();
    }
}
```
