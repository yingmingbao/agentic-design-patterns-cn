package com.strategist.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.strategist.ai.entity.SysAgentChatDialogDetail;
import com.strategist.ai.mapper.SysAgentChatDialogDetailMapper;
import com.strategist.ai.service.SysAgentChatDialogDetailService;
import com.strategist.ai.util.UserContext;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SysAgentChatDialogDetailServiceImpl extends ServiceImpl<SysAgentChatDialogDetailMapper, SysAgentChatDialogDetail> implements SysAgentChatDialogDetailService {

    @Override
    public void saveMessage(Long dialogId, String role, String content, String source) {
        saveMessage(dialogId, role, content, source, "chat", null);
    }

    @Override
    public void saveMessage(Long dialogId, String role, String content, String source, String type, String reasoningContent) {
        SysAgentChatDialogDetail detail = new SysAgentChatDialogDetail();
        detail.setDialogId(dialogId);
        detail.setRole(role);
        detail.setContent(content);
        detail.setSource(source);
        detail.setType(type);
        detail.setReasoningContent(reasoningContent);
        detail.setCreateBy(UserContext.getUsername());
        detail.setUpdateBy(UserContext.getUsername());
        detail.setCreateTime(LocalDateTime.now());
        detail.setUpdateTime(LocalDateTime.now());
        detail.setIsDeleted(0);
        detail.setIsLiked(0);
        detail.setIsDisliked(0);
        this.save(detail);
    }

    @Override
    public boolean updateFeedback(Long detailId, Integer isLiked, Integer isDisliked) {
        SysAgentChatDialogDetail detail = this.getById(detailId);
        if (detail != null) {
            if (isLiked != null) detail.setIsLiked(isLiked);
            if (isDisliked != null) detail.setIsDisliked(isDisliked);
            detail.setUpdateBy(UserContext.getUsername());
            detail.setUpdateTime(LocalDateTime.now());
            return this.updateById(detail);
        }
        return false;
    }

    @Override
    public List<SysAgentChatDialogDetail> getDialogHistory(Long dialogId) {
        return this.list(new LambdaQueryWrapper<SysAgentChatDialogDetail>()
                .eq(SysAgentChatDialogDetail::getDialogId, dialogId)
                .orderByAsc(SysAgentChatDialogDetail::getCreateTime));
    }
}
