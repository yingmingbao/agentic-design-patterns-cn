# Spring AI 对 Model Context Protocol (MCP) 的支持。

## POM依赖

``` java
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-mcp-client-spring-boot-starter</artifactId>
    <version>1.0.0-M6</version>
</dependency>

<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-mcp-client</artifactId>
    <version>1.0.0-M6</version>
</dependency>

<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-mcp-spring</artifactId>
    <version>1.0.0-M6</version>
</dependency>

```

## 2. Java 代码实现
在 Java 中，我们通常使用 McpToolset 或直接将 MCP Client 注册为 ChatClient 的工具。

``` java
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.client.McpClient;
import org.springframework.ai.mcp.client.stdio.StdioServerParameters;
import org.springframework.ai.mcp.spring.McpToolset;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;

@Configuration
public class McpConfig {

    // 获取目标文件夹的绝对路径
    private final String targetFolderPath = Paths.get("").toAbsolutePath()
            .resolve("mcp_managed_files").toString();

    @Bean
    public ChatClient filesystemAssistant(ChatClient.Builder builder, McpClient mcpClient) {
        // 确保目录存在
        new File(targetFolderPath).mkdirs();

        return builder
                .defaultSystem("Help the user manage their files. You can list, read, and write files. " +
                               "You are operating in the following directory: " + targetFolderPath)
                // 将 MCP 工具集注入到 ChatClient 中
                .defaultTools(new McpToolset(mcpClient))
                .build();
    }

    @Bean
    public StdioServerParameters mcpServerParameters() {
        // 对应 Python 中的 StdioServerParameters
        return StdioServerParameters.builder("npx")
                .args("-y", "@modelcontextprotocol/server-filesystem", targetFolderPath)
                .build();
    }
}
```

## 关键点提示：

路径一致性：Java 中的 Paths.get("").toAbsolutePath() 相当于 Python 的 os.path.abspath(__file__) 所在的当前工作目录。

Node.js 环境：确保你的机器上安装了 Node.js，因为 npx 命令是 MCP 文件系统服务器的标准启动方式。

Spring AI Alibaba 适配：如果你使用的是 Spring AI Alibaba 的 DashScope (通义千问) 模型，其 API 完全兼容 Spring AI 标准，上述代码中的 ChatClient 会自动调用 DashScope 后端。
