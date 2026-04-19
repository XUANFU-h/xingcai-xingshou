package com.music.scoring_system.config;

import com.music.scoring_system.util.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 放行所有的跨域预检请求 (OPTIONS)
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String requestURI = request.getRequestURI();

        // =========================================================
        // 🚨 终极核心修复：暴力放行所有终端基础运行所必需的接口
        // =========================================================
        if ("/api/admin/contestants".equals(requestURI) && "GET".equalsIgnoreCase(request.getMethod())) {
            return true; // 绿灯：拉取选手数据
        }
        if ("/api/staff/register".equals(requestURI) ||
                "/api/tactics/public/groups".equals(requestURI)) {
            return true; // 绿灯：自主注册、获取组别均无需 Token！
        }

        // 🚀 安全探针：登录接口触发时，刷新系统存活时间戳
        if ("/api/scores/auth".equals(requestURI)) {
            redisTemplate.opsForValue().set("sys:last_admin_active_time", String.valueOf(System.currentTimeMillis()));
            return true;
        }

        // 2. 从 HTTP 请求头中获取 Authorization 字段
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7); // 提取出真实的 Token 字符串
            try {
                // 3. 验证 Token
                if (jwtUtils.validateToken(token)) {
                    // 🚀 安全探针：任何合法授权请求通过，均刷新系统存活时间戳
                    redisTemplate.opsForValue().set("sys:last_admin_active_time", String.valueOf(System.currentTimeMillis()));
                    return true; // 验证通过，放行到具体的 Controller
                }
            } catch (Exception e) {
                // Token 过期或异常时防止 500，走底部的 401 拦截
            }
        }

        // 4. 验证失败（没有 Token 或 Token 失效），直接拦截并返回 401 状态码
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"error\": \"未授权或登录已过期，请重新在导播台登录验证\"}");
        return false;
    }
}