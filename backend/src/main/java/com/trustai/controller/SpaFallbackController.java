package com.trustai.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnResource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * SPA（单页应用）回退控制器。
 *
 * <p>当 Spring Boot 同时承载 Vue 前端静态资源时，所有非 API、非静态资源的路由
 * 都应返回 {@code index.html}，由 Vue Router 在前端处理路由。
 *
 * <p>此控制器仅在 {@code classpath:static/index.html} 存在时生效
 * （即打包模式：执行 {@code npm run build} 后将 {@code dist/} 内容复制到
 * {@code backend/src/main/resources/static/}）。
 *
 * <p>开发模式下，前端运行在独立的 Vite 开发服务器（通常为 {@code http://localhost:5173}），
 * 本控制器不会被触发。
 */
@Controller
@ConditionalOnResource(resources = "classpath:static/index.html")
public class SpaFallbackController {

    /**
     * 将所有非 API、非静态资源路径转发至 {@code index.html}。
     *
     * <p>匹配规则（按优先级）：
     * <ul>
     *   <li>{@code /api/**}          → 由各 RestController 处理，不在此匹配</li>
     *   <li>{@code /uploads/**}      → 由静态资源处理器服务，不在此匹配</li>
     *   <li>{@code /h2-console/**}   → 由 H2 控制台处理，不在此匹配</li>
     *   <li>{@code /swagger-ui/**}   → 由 Springdoc 处理，不在此匹配</li>
     *   <li>其他所有路径             → 转发至 {@code /index.html}</li>
     * </ul>
     *
     * <p>路由段限制：最多支持 5 级路径（如 {@code /a/b/c/d/e}），可按需扩展。
     */
    @RequestMapping(value = {
            "/{p1:[^\\.]*}",
            "/{p1:[^\\.]*}/{p2:[^\\.]*}",
            "/{p1:[^\\.]*}/{p2:[^\\.]*}/{p3:[^\\.]*}",
            "/{p1:[^\\.]*}/{p2:[^\\.]*}/{p3:[^\\.]*}/{p4:[^\\.]*}",
            "/{p1:[^\\.]*}/{p2:[^\\.]*}/{p3:[^\\.]*}/{p4:[^\\.]*}/{p5:[^\\.]*}"
    })
    public String forward(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String uri = request.getRequestURI();
        // API 路径、上传文件、H2 控制台、Swagger 由各自处理器接管，不做转发
        if (uri.startsWith("/api/") || uri.startsWith("/uploads/")
                || uri.startsWith("/h2-console") || uri.startsWith("/swagger-ui")
                || uri.startsWith("/v3/api-docs")) {
            // 显式返回 404，避免 Spring MVC 因 return null 尝试解析视图而产生
            // "No static resource ..." 日志噪音或意外 500 响应
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }
        return "forward:/index.html";
    }
}
