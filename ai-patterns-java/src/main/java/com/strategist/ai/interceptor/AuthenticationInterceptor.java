package com.strategist.ai.interceptor;

import com.strategist.ai.entity.User;
import com.strategist.ai.mapper.UserMapper;
import com.strategist.ai.util.JwtUtil;
import com.strategist.ai.util.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 认证拦截器
 */
@Component
public class AuthenticationInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserMapper userMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 如果是OPTIONS请求，直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
                return true;
            }
            String token = request.getHeader("Authorization");
            
            // Backdoor for testing
            if ("Bearer dev".equals(token)) {
                UserContext.setUserId(1L);
                UserContext.setUsername("admin");
                UserContext.setTenantId(1L);
                UserContext.setEnterpriseId(1L);
                return true;
            }

            if (StringUtils.hasText(token) && token.startsWith("Bearer ")) {
            token = token.substring(7);
            try {
                if (jwtUtil.validateToken(token)) {
                    Long userId = jwtUtil.getUserIdFromToken(token);
                    UserContext.setUserId(userId);
                    try {
                        User user = userMapper.selectById(userId);
                        if (null != user) {
                            Long enterpriseId = user.getEnterpriseId();
                            UserContext.setEnterpriseId(enterpriseId);
                            UserContext.setTenantId(user.getTenantId());
                            UserContext.setUsername(user.getUsername());
                        } else {
                            // Default or error handling
                        }
                    } catch (Exception e) {
                        // logger.warn("Failed to get user info", e);
                    }
                    return true;
                }
            } catch (Exception e) {
                // logger.error("Token validation failed", e);
            }
        }
        
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        return false;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserContext.clear();
    }
}
