CREATE DATABASE `demo_db` /*!40100 COLLATE 'utf8mb4_0900_ai_ci' */
USE demo_db;


CREATE TABLE `sys_user` (
                            `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
                            `username` VARCHAR(50) NOT NULL COMMENT '用户名' COLLATE 'utf8mb4_unicode_ci',
                            `password` VARCHAR(255) NOT NULL COMMENT '密码' COLLATE 'utf8mb4_unicode_ci',
                            `nickname` VARCHAR(50) NULL DEFAULT NULL COMMENT '昵称' COLLATE 'utf8mb4_unicode_ci',
                            `email` VARCHAR(100) NULL DEFAULT NULL COMMENT '邮箱' COLLATE 'utf8mb4_unicode_ci',
                            `phone` VARCHAR(20) NULL DEFAULT NULL COMMENT '手机号' COLLATE 'utf8mb4_unicode_ci',
                            `avatar` VARCHAR(255) NULL DEFAULT NULL COMMENT '头像' COLLATE 'utf8mb4_unicode_ci',
                            `status` TINYINT NULL DEFAULT '1' COMMENT '状态：0-禁用 1-正常',
                            `team_id` BIGINT NULL DEFAULT NULL COMMENT '所属团队ID',
                            `deleted` TINYINT NULL DEFAULT '0' COMMENT '删除标记：0-未删除 1-已删除',
                            `create_time` DATETIME NULL DEFAULT (CURRENT_TIMESTAMP) COMMENT '创建时间',
                            `update_time` DATETIME NULL DEFAULT (CURRENT_TIMESTAMP) ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                            `create_by` BIGINT NULL DEFAULT NULL COMMENT '创建人',
                            `update_by` BIGINT NULL DEFAULT NULL COMMENT '更新人',
                            `theme_config` TEXT NULL DEFAULT NULL COMMENT '用户主题配置(JSON)' COLLATE 'utf8mb4_unicode_ci',
                            `tenant_id` BIGINT NULL DEFAULT '0' COMMENT 'Tenant ID',
                            `enterprise_id` BIGINT NULL DEFAULT NULL COMMENT '企业主键ID',
                            PRIMARY KEY (`id`) USING BTREE,
                            UNIQUE INDEX `uk_username` (`username`) USING BTREE,
                            INDEX `idx_user_tenant` (`tenant_id`) USING BTREE
)
    COMMENT='用户表'
COLLATE='utf8mb4_unicode_ci'
ENGINE=InnoDB
AUTO_INCREMENT=1
;


CREATE TABLE `sys_agent_chat_dialog` (
                                         `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                         `tenant_id` BIGINT NULL DEFAULT NULL COMMENT '租户ID',
                                         `enterprise_id` BIGINT NULL DEFAULT NULL COMMENT '企业ID',
                                         `user_id` BIGINT NULL DEFAULT NULL COMMENT '用户ID',
                                         `title` VARCHAR(255) NULL DEFAULT NULL COMMENT '对话标题' COLLATE 'utf8mb4_general_ci',
                                         `type` VARCHAR(50) NULL DEFAULT 'chat' COMMENT '对话类型: chat-普通对话, deepsearch-深度思考' COLLATE 'utf8mb4_general_ci',
                                         `is_deleted` TINYINT(1) NULL DEFAULT '0' COMMENT '是否删除 0:否 1:是',
                                         `create_time` DATETIME NULL DEFAULT (CURRENT_TIMESTAMP) COMMENT '创建时间',
                                         `update_time` DATETIME NULL DEFAULT (CURRENT_TIMESTAMP) ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                         `create_by` VARCHAR(64) NULL DEFAULT NULL COMMENT '创建者' COLLATE 'utf8mb4_general_ci',
                                         `update_by` VARCHAR(64) NULL DEFAULT NULL COMMENT '更新者' COLLATE 'utf8mb4_general_ci',
                                         `remark` VARCHAR(500) NULL DEFAULT NULL COMMENT '备注' COLLATE 'utf8mb4_general_ci',
                                         PRIMARY KEY (`id`) USING BTREE
)
COMMENT='智能体对话表'
COLLATE='utf8mb4_general_ci'
ENGINE=InnoDB
AUTO_INCREMENT=1
;


CREATE TABLE `sys_agent_chat_dialog_detail` (
                                                `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                                `dialog_id` BIGINT NOT NULL COMMENT '对话ID',
                                                `role` VARCHAR(20) NOT NULL COMMENT '角色(user/assistant)' COLLATE 'utf8mb4_general_ci',
                                                `content` TEXT NULL DEFAULT NULL COMMENT '内容' COLLATE 'utf8mb4_general_ci',
                                                `is_deleted` TINYINT(1) NULL DEFAULT '0' COMMENT '是否删除 0:否 1:是',
                                                `create_time` DATETIME NULL DEFAULT (CURRENT_TIMESTAMP) COMMENT '创建时间',
                                                `update_time` DATETIME NULL DEFAULT (CURRENT_TIMESTAMP) ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                                `create_by` VARCHAR(64) NULL DEFAULT NULL COMMENT '创建者' COLLATE 'utf8mb4_general_ci',
                                                `update_by` VARCHAR(64) NULL DEFAULT NULL COMMENT '更新者' COLLATE 'utf8mb4_general_ci',
                                                `remark` VARCHAR(500) NULL DEFAULT NULL COMMENT '备注' COLLATE 'utf8mb4_general_ci',
                                                `source` VARCHAR(50) NULL DEFAULT NULL COMMENT '模型来源' COLLATE 'utf8mb4_general_ci',
                                                `type` VARCHAR(50) NULL DEFAULT 'chat' COMMENT '对话类型: chat-普通对话, deepsearch-深度思考' COLLATE 'utf8mb4_general_ci',
                                                `reasoning_content` LONGTEXT NULL DEFAULT NULL COMMENT '思考过程' COLLATE 'utf8mb4_general_ci',
                                                `is_liked` TINYINT(1) NULL DEFAULT '0' COMMENT '是否点赞 0:否 1:是',
                                                `is_disliked` TINYINT(1) NULL DEFAULT '0' COMMENT '是否点踩 0:否 1:是',
                                                PRIMARY KEY (`id`) USING BTREE,
                                                INDEX `idx_dialog_id` (`dialog_id`) USING BTREE
)
    COMMENT='智能体对话详情表'
COLLATE='utf8mb4_general_ci'
ENGINE=InnoDB
AUTO_INCREMENT=1
;
CREATE TABLE `sys_deep_research` (
                                     `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                     `topic` VARCHAR(500) NULL DEFAULT NULL COMMENT '研究主题' COLLATE 'utf8mb4_0900_ai_ci',
                                     `reasoning_content` LONGTEXT NULL DEFAULT NULL COMMENT '思考过程' COLLATE 'utf8mb4_0900_ai_ci',
                                     `content` LONGTEXT NULL DEFAULT NULL COMMENT '研究报告内容' COLLATE 'utf8mb4_0900_ai_ci',
                                     `tenant_id` BIGINT NULL DEFAULT NULL COMMENT '租户ID',
                                     `user_id` BIGINT NULL DEFAULT NULL COMMENT '用户ID',
                                     `enterprise_id` BIGINT NULL DEFAULT NULL COMMENT '企业ID',
                                     `is_deleted` TINYINT(1) NULL DEFAULT '0' COMMENT '是否删除 0-未删除 1-已删除',
                                     `create_by` VARCHAR(64) NULL DEFAULT '' COMMENT '创建者' COLLATE 'utf8mb4_0900_ai_ci',
                                     `create_time` DATETIME NULL DEFAULT NULL COMMENT '创建时间',
                                     `update_by` VARCHAR(64) NULL DEFAULT '' COMMENT '更新者' COLLATE 'utf8mb4_0900_ai_ci',
                                     `update_time` DATETIME NULL DEFAULT NULL COMMENT '更新时间',
                                     PRIMARY KEY (`id`) USING BTREE,
                                     INDEX `idx_tenant_id` (`tenant_id`) USING BTREE,
                                     INDEX `idx_user_id` (`user_id`) USING BTREE,
                                     INDEX `idx_enterprise_id` (`enterprise_id`) USING BTREE
)
    COMMENT='深度研究表'
COLLATE='utf8mb4_0900_ai_ci'
ENGINE=InnoDB
AUTO_INCREMENT=1
;
