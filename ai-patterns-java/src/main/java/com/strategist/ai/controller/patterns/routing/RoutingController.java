package com.strategist.ai.controller.patterns.routing;

import com.strategist.ai.common.Result;
import com.strategist.ai.patterns.routing.QuestionComponent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/ai/routing")
@Slf4j
public class RoutingController {

    @Resource
    private QuestionComponent questionComponent;

    @GetMapping("/test")
    public Result<String> test(String rawInput) throws Exception {
        log.info("runFullChain rawInput:{}", rawInput);
        questionComponent.run(new String[]{});
        return Result.success("test success");
    }

}
