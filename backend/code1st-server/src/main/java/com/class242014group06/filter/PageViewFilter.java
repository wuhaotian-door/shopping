package com.class242014group06.filter;

import com.class242014group06.entity.PageView;
import com.class242014group06.mapper.PageViewMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.LocalDateTime;

/**
 * 页面浏览记录过滤器 — 记录每次 GET 请求到 page_view 表
 */
@Component
public class PageViewFilter implements Filter {

    @Autowired
    private PageViewMapper pageViewMapper;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        chain.doFilter(request, response);

        try {
            HttpServletRequest req = (HttpServletRequest) request;
            // 只记录 GET 请求的页面访问，跳过静态资源和 API
            if ("GET".equalsIgnoreCase(req.getMethod())) {
                String path = req.getRequestURI();
                // 跳过静态资源、upload、api 数据请求
                if (!path.startsWith("/images/") && !path.startsWith("/models/")
                        && !path.startsWith("/videos/") && !path.startsWith("/uploads/")
                        && !path.startsWith("/webjars/") && !path.startsWith("/doc.html")
                        && !path.startsWith("/favicon.ico") && !path.endsWith(".js")
                        && !path.endsWith(".css") && !path.endsWith(".png")
                        && !path.endsWith(".jpg") && !path.endsWith(".ico")
                        && !path.endsWith(".glb") && !path.endsWith(".gltf")
                        && !path.endsWith(".mp4") && !path.endsWith(".webm")) {
                    String ip = req.getHeader("X-Forwarded-For");
                    if (ip == null || ip.isEmpty()) {
                        ip = req.getRemoteAddr();
                    }
                    pageViewMapper.insert(PageView.builder()
                            .path(path)
                            .ip(ip)
                            .createTime(LocalDateTime.now())
                            .build());
                }
            }
        } catch (Exception e) {
            // 记录失败不影响正常请求
        }
    }
}
