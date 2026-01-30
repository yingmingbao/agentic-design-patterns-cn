package com.strategist.ai.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.strategist.ai.entity.SysAgentChatDialogDetail;

import java.util.List;

public interface SysAgentChatDialogDetailService extends IService<SysAgentChatDialogDetail> {
    void saveMessage(Long dialogId, String role, String content, String source);
    void saveMessage(Long dialogId, String role, String content, String source, String type, String reasoningContent);
    boolean updateFeedback(Long detailId, Integer isLiked, Integer isDisliked);
    List<SysAgentChatDialogDetail> getDialogHistory(Long dialogId);
}
