package com.strategist.ai.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.strategist.ai.common.Result;
import com.strategist.ai.controller.chat.dto.AiChatDto;
import com.strategist.ai.entity.SysAgentChatDialog;
import com.strategist.ai.entity.SysAgentChatDialogDetail;
import com.strategist.ai.service.SysAgentChatDialogDetailService;
import com.strategist.ai.service.SysAgentChatDialogService;
import com.strategist.ai.service.component.AiServiceComponent;
import com.strategist.ai.service.enums.AIChatEnum;
import com.strategist.ai.util.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/ai/chat")
@Slf4j
public class AgentChatController {


    @Autowired
    private SysAgentChatDialogService dialogService;

    @Autowired
    private SysAgentChatDialogDetailService detailService;

    @Autowired
    private Map<String, AiServiceComponent> aiServices;

    private AiServiceComponent getAiService(String type) {
        if (type == null || type.isEmpty()) {
            return aiServices.get("deepSeekComponent");
        }
        switch (AIChatEnum.getByName(type.toLowerCase())) {
            case doubao:
                return aiServices.get("doubaoAIComponent");
            case bailian:
                return aiServices.get("baiLianComponent");
            case deepsearch:
                return aiServices.get("deepSearchComponent");
            case deepseek:
                return aiServices.get("deepSeekComponent");
            default:
                return aiServices.get("deepSeekComponent");
        }
    }

    // Inner class to hold stream context


    /**
     * 获取对话列表
     */
    @GetMapping("/list")
    public Result<List<SysAgentChatDialog>> list() {
        Long userId = UserContext.getUserId();
        List<SysAgentChatDialog> list = dialogService.list(new LambdaQueryWrapper<SysAgentChatDialog>()
                .eq(SysAgentChatDialog::getUserId, userId)
                .orderByDesc(SysAgentChatDialog::getCreateTime));
        return Result.success(list);
    }

    /**
     * 创建新对话
     */
    @PostMapping("/create")
    public Result<Long> create(@RequestBody Map<String, String> params) {
        String title = params.getOrDefault("title", "新对话");
        Long id = dialogService.createDialog(title);
        return Result.success(id);
    }

    /**
     * 重命名对话
     */
    @PostMapping("/rename")
    public Result<Boolean> rename(@RequestBody Map<String, Object> params) {
        Long dialogId = Long.parseLong(params.get("dialogId").toString());
        String title = params.get("title").toString();
        return Result.success(dialogService.updateTitle(dialogId, title));
    }

    /**
     * 更新对话标题
     */
    @PostMapping("/updateTitle")
    public Result<Boolean> updateTitle(@RequestBody Map<String, Object> params) {
        Long dialogId = Long.parseLong(params.get("dialogId").toString());
        String title = params.get("title").toString();

        SysAgentChatDialog dialog = new SysAgentChatDialog();
        dialog.setId(dialogId);
        dialog.setTitle(title);
        return Result.success(dialogService.updateById(dialog));
    }

    /**
     * 删除对话
     */
    @PostMapping("/delete")
    public Result<Boolean> delete(@RequestBody Map<String, Object> params) {
        Long dialogId = Long.parseLong(params.get("dialogId").toString());
        return Result.success(dialogService.removeById(dialogId));
    }

    /**
     * 更新反馈 (点赞/点踩)
     */
    @PostMapping("/feedback")
    public Result<Boolean> feedback(@RequestBody Map<String, Object> params) {
        Long detailId = Long.parseLong(params.get("detailId").toString());
        Integer isLiked = params.containsKey("isLiked") ? Integer.parseInt(params.get("isLiked").toString()) : null;
        Integer isDisliked = params.containsKey("isDisliked") ? Integer.parseInt(params.get("isDisliked").toString()) : null;

        return Result.success(detailService.updateFeedback(detailId, isLiked, isDisliked));
    }

    /**
     * 获取对话历史
     */
    @GetMapping("/history")
    public Result<List<SysAgentChatDialogDetail>> history(@RequestParam Long dialogId) {
        return Result.success(detailService.getDialogHistory(dialogId));
    }

    /**
     * 发送消息 (Blocking)
     */
    @PostMapping("/send")
    public Result<String> send(@RequestBody AiChatDto params) {
        return Result.success(getAiService(params.getType()).send(params));
    }

    /**
     * Stop generation
     */
    @PostMapping("/stop")
    public Result<Boolean> stop(@RequestBody AiChatDto params) {
        return Result.success(getAiService(params.getType()).stop(params));
    }

    /**
     * Streaming Endpoint (Server-Sent Events)
     * Supports resuming existing streams if active.
     */
    @PostMapping("/stream")
    public SseEmitter stream(@RequestBody AiChatDto params) {
        return getAiService(params.getType()).createEmitter(params);
    }

}
