package com.strategist.ai.patterns.mcp;

import org.springframework.ai.chat.client.ChatClient;
//import org.springframework.ai.mcp.client.httpclient.McpClient;
//import org.springframework.ai.mcp.client.stdio.StdioServerParameters;
//import org.springframework.ai.mcp.spring.McpToolset;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;
import java.io.File;
import java.nio.file.Paths;

// @Configuration
public class McpConfig {

//    @Resource
//    McpClient mcpClient;
//
//    // 获取目标文件夹的绝对路径
//    private final String targetFolderPath = Paths.get("").toAbsolutePath()
//            .resolve("mcp_managed_files").toString();
//
//    @Bean
//    public ChatClient filesystemAssistant(ChatClient.Builder builder, McpClient mcpClient) {
//        // 确保目录存在
//        new File(targetFolderPath).mkdirs();
//
//        return builder
//                .defaultSystem("Help the user manage their files. You can list, read, and write files. " +
//                        "You are operating in the following directory: " + targetFolderPath)
//                // 将 MCP 工具集注入到 ChatClient 中
//                .defaultTools(new McpToolset(mcpClient))
//                .build();
//    }
//
//    @Bean
//    public StdioServerParameters mcpServerParameters() {
//        // 对应 Python 中的 StdioServerParameters
//        return StdioServerParameters.builder("npx")
//                .args("-y", "@modelcontextprotocol/server-filesystem", targetFolderPath)
//                .build();
//    }
}
