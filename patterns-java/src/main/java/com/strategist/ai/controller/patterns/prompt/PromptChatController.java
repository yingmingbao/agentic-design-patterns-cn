package com.strategist.ai.controller.patterns.prompt;


import com.strategist.ai.common.Result;
import com.strategist.ai.patterns.prompt.ExtractionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/ai/chat")
@Slf4j
public class PromptChatController {


    @Resource
    private ExtractionService extractionService;


    @GetMapping("/rawInputSpec")
    public Result<String> runFullChain(String rawInput) {
        log.info("runFullChain rawInput:{}", rawInput);
        return Result.success(extractionService.runFullChain(rawInput));
    }
}
