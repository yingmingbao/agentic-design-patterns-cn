package com.strategist.ai.controller.patterns;

import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.strategist.ai.common.Result;
import com.strategist.ai.patterns.errorhandle.RobustLocationAgent;
import com.strategist.ai.patterns.errorhandle.dto.AddressDTO;
import com.strategist.ai.patterns.goal.CodeAgentService;
import com.strategist.ai.patterns.goal.dto.GoalSettingDTO;
import com.strategist.ai.patterns.planning.ArticleCrewPlanningService;
import com.strategist.ai.service.component.DeepResearchAgentService;
import com.strategist.ai.patterns.planning.DeepSeekResearchService;
import com.strategist.ai.patterns.reflection.ReflectionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import javax.annotation.Resource;
import java.io.IOException;

@RestController
@Slf4j
@RequestMapping("/ai")
public class ReflectionController {

    @Resource
    ReflectionService reflectionService;

    @Resource
    ArticleCrewPlanningService articleCrewPlanningService;

    @Resource
    DeepSeekResearchService deepSeekResearchService;

    @Resource
    DeepResearchAgentService deepResearchAgentService;

    @Resource
    CodeAgentService codeAgentService;


    @Resource
    RobustLocationAgent robustLocationAgent;


    @GetMapping("/reflection")
    public Result<String> runReflectionChain(String topic) {
        reflectionService.runReflectionLoop(topic, 5);
        return Result.success();
    }

    @GetMapping("/planning")
    public Result<String> runPlanningChain(String topic) throws GraphRunnerException {

        return Result.success(articleCrewPlanningService.runArticleTask("如何开发Agent"));
    }

    @GetMapping("/deepsearch")
    public Result<String> deepsearch(String topic) throws GraphRunnerException {

        return Result.success(deepSeekResearchService.conductResearch(topic));
    }

    @GetMapping("/goal")
    public Result<String> goal(@RequestBody GoalSettingDTO goalSettingDTO) throws GraphRunnerException {

        String result;
        try {
            result = codeAgentService.runCodeAgent(goalSettingDTO);

        } catch (Exception e) {
            throw new GraphRunnerException(e.getMessage());
        }
        return Result.success(result);
    }

    @GetMapping("/robust")
    public Result<String> robustLocationAgent(@RequestBody AddressDTO addressDTO) throws GraphRunnerException {

        String result;
        try {
            result = robustLocationAgent.execute(addressDTO.getAddress());

        } catch (Exception e) {
            throw new GraphRunnerException(e.getMessage());
        }
        return Result.success(result);
    }

}
