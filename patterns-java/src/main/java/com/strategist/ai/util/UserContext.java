package com.strategist.ai.util;

/**
 * 用户上下文工具类
 * 用于在当前线程中存储和获取用户信息
 */
public class UserContext {
    private static final ThreadLocal<Long> USER_ID_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<String> USERNAME_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<Long> TENANT_ID_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<Long> ENTERPRISE_ID_HOLDER = new ThreadLocal<>();

    /**
     * 设置当前用户ID
     */
    public static void setUserId(Long userId) {
        USER_ID_HOLDER.set(userId);
    }

    /**
     * 获取当前用户ID
     */
    public static Long getUserId() {
        return USER_ID_HOLDER.get();
    }

    /**
     * 设置当前用户名
     */
    public static void setUsername(String username) {
        USERNAME_HOLDER.set(username);
    }

    /**
     * 获取当前用户名
     */
    public static String getUsername() {
        return USERNAME_HOLDER.get();
    }

    /**
     * 设置当前租户ID
     */
    public static void setTenantId(Long tenantId) {
        TENANT_ID_HOLDER.set(tenantId);
    }

    /**
     * 获取当前租户ID
     */
    public static Long getTenantId() {
        return TENANT_ID_HOLDER.get();
    }

    /**
     * 设置当前企业ID
     */
    public static void setEnterpriseId(Long enterpriseId) {
        ENTERPRISE_ID_HOLDER.set(enterpriseId);
    }

    /**
     * 获取当前企业ID
     */
    public static Long getEnterpriseId() {
        return ENTERPRISE_ID_HOLDER.get();
    }

    /**
     * 清除当前用户信息
     */
    public static void clear() {
        USER_ID_HOLDER.remove();
        USERNAME_HOLDER.remove();
        TENANT_ID_HOLDER.remove();
        ENTERPRISE_ID_HOLDER.remove();
    }
}
