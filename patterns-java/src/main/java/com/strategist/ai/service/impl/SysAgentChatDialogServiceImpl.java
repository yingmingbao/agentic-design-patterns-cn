package com.strategist.ai.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.strategist.ai.entity.SysAgentChatDialog;
import com.strategist.ai.mapper.SysAgentChatDialogMapper;
import com.strategist.ai.service.SysAgentChatDialogService;
import com.strategist.ai.util.UserContext;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class SysAgentChatDialogServiceImpl extends ServiceImpl<SysAgentChatDialogMapper, SysAgentChatDialog> implements SysAgentChatDialogService {

    @Override
    public Long createDialog(String title) {
        SysAgentChatDialog dialog = new SysAgentChatDialog();
        dialog.setTitle(title);
        dialog.setUserId(UserContext.getUserId());
        dialog.setTenantId(UserContext.getTenantId());
        dialog.setEnterpriseId(UserContext.getEnterpriseId());
        dialog.setCreateBy(UserContext.getUsername());
        dialog.setUpdateBy(UserContext.getUsername());
        dialog.setCreateTime(LocalDateTime.now());
        dialog.setUpdateTime(LocalDateTime.now());
        dialog.setIsDeleted(0);
        this.save(dialog);
        return dialog.getId();
    }

    @Override
    public boolean updateTitle(Long dialogId, String title) {
        SysAgentChatDialog dialog = this.getById(dialogId);
        if (dialog != null) {
            dialog.setTitle(title);
            dialog.setUpdateTime(LocalDateTime.now());
            return this.updateById(dialog);
        }
        return false;
    }
}
