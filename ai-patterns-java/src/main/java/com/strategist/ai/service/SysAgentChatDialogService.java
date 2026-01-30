package com.strategist.ai.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.strategist.ai.entity.SysAgentChatDialog;

public interface SysAgentChatDialogService extends IService<SysAgentChatDialog> {
    Long createDialog(String title);
    boolean updateTitle(Long dialogId, String title);
}
