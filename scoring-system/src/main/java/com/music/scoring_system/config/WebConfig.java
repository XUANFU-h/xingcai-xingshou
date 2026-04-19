package com.music.scoring_system.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    // 直接硬编码路径，确保万无一失
    private final String uploadDir = "/home/scoring_uploads/";

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 当访问 /uploads/xxx.jpg 时，Spring Boot 会自动去 Linux 物理路径下找
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadDir);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 放行所有跨域请求，避免前后端分离时代的遗留问题
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}