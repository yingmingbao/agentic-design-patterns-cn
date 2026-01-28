
在 Spring AI Alibaba 的体系中，“流水线模式”通常被称为 Prompt Chaining。它将一个复杂的、容易让 LLM 产生幻觉的大任务，拆解为多个确定性更高的微任务。

虽然 Spring AI 没有 Python 版 LangChain 那种 | 运算符，但它通过 ChatClient 的流式 API 和 Java 函数式编程 提供了更严谨的实现方式。

# 模式实现：任务拆解流水线

假设我们要实现你提到的两步走任务：提取 (Extraction) -> 转换 (Transformation)。

## 1. 核心架构设计
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

## 2. Spring AI 流水线模式的优势
相比于单一的长 Prompt，这种流水线模式有以下三个核心优势：

隔离关注点 (Separation of Concerns)： 第一步只负责提取，第二步只负责排版。你可以为第一步设置较低的 temperature（追求稳定），为第二步设置较高的 temperature（追求文采）。

中间结果校验： 在 Java 代码中，你可以在 extractStep 之后加入逻辑：如果提取的内容为空，则直接报错或返回默认值，而不必浪费 Token 进行第二步。

可观测性： Spring AI Alibaba 深度集成了 Spring Cloud Sleuth / Micrometer。你可以清晰地看到流水线中哪一个步骤耗时最长，或者哪一个模型调用失败了。


## 3. 进阶：使用 Advisor 增强流水线

如果你希望流水线具备某种“记忆”或“拦截”能力，Spring AI 提供了 Advisor 模式，这相当于 LCEL 中的中间件：
``` java
return chatClient.prompt()
        .advisors(new MessageChatMemoryAdvisor(chatMemory)) // 注入上下文记忆
        .user(u -> u.text("处理数据...").param("data", data))
        .call()
        .content();

```

## 总结
在 Spring AI Alibaba 中，“流水线模式” = ChatClient + 显式的参数传递。

LangChain LCEL: 强调“链”本身的声明式定义。
Spring AI: 强调“客户端”的功能组合，通过 prompt() 方法的链式调用，配合 Java 原生的 andThen() 或方法拆解来实现。
