package com.trustai.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置。
 *
 * <p>CORS 策略已由 {@link com.trustai.config.SecurityConfig#corsConfigurationSource()} 统一配置，
 * 通过 Spring Security 的 CORS 过滤器在安全过滤链最前端处理。
 * 此处不再重复注册 {@code addCorsMappings}，避免双重配置导致响应中出现
 * 重复的 {@code Access-Control-Allow-Origin} 头而引发浏览器 CORS 拒绝。
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Autowired
    private RateLimitInterceptor rateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/login", "/api/auth/logout");
    }
}
