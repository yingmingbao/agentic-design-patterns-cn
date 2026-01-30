package com.strategist.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 智能体对话详情表
 */
@Data
@TableName("sys_agent_chat_dialog_detail")
public class SysAgentChatDialogDetail implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 对话ID
     */
    private Long dialogId;

    /**
     * 角色(user/assistant)
     */
    private String role;

    /**
     * 内容
     */
    private String content;

    /**
     * 模型来源
     */
    private String source;

    /**
     * 对话类型: chat-普通对话, deepsearch-深度思考
     */
    private String type;

    /**
     * 思考过程
     */
    private String reasoningContent;

    /**
     * 是否点赞 0:否 1:是
     */
    private Integer isLiked;

    /**
     * 是否点踩 0:否 1:是
     */
    private Integer isDisliked;

    /**
     * 是否删除 0:否 1:是
     */
    @TableLogic
    private Integer isDeleted;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 创建者
     */
    private String createBy;

    /**
     * 更新者
     */
    private String updateBy;

    /**
     * 备注
     */
    private String remark;
}
