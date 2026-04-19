package com.music.scoring_system.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private AdminAuthInterceptor adminAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminAuthInterceptor)
                // 👇 这些核心高危接口，没有 Token 绝对进不去！
                .addPathPatterns(
                        "/api/tactics/**",
                        "/api/admin/**",
                        "/api/scores/kick-cadre",
                        "/api/scores/update-pwd",
                        "/api/scores/set-*",
                        "/api/scores/emergency-lock",
                        "/api/scores/manual-update**",
                        "/api/scores/clear-all",
                        "/api/scores/switch-session",
                        "/api/scores/delete-session"
                );
        // 没有被 addPathPatterns 包含的接口 (如 /api/scores/submit 观众打分) 默认放行
    }
}