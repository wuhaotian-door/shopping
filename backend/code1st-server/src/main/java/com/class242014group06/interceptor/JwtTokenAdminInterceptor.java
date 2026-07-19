package com.class242014group06.interceptor;


import com.class242014group06.config.ServerStartupTracker;
import com.class242014group06.constant.JwtClaimsConstant;
import com.class242014group06.context.BaseContext;
import com.class242014group06.properties.JwtProperties;
import com.class242014group06.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;

/**
 * jwt令牌校验的拦截器
 * 每次后端重启后，之前签发的 token 会因 iat < 服务启动时间而被拒绝，强制所有角色重新登录
 */
@Component
@Slf4j
public class JwtTokenAdminInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private ServerStartupTracker serverStartupTracker;

    /**
     * 校验jwt
     */
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //判断当前拦截到的是Controller的方法还是其他资源
        if (!(handler instanceof HandlerMethod)) {
            //当前拦截到的不是动态方法，直接放行
            return true;
        }

        //1、从请求头中获取令牌
        String token = request.getHeader(jwtProperties.getAdminTokenName());

        //2、校验令牌
        try {
            log.info("jwt校验:{}", token);
            Claims claims = JwtUtil.parseJWT(jwtProperties.getAdminSecretKey(), token);

            // 校验 token 签发时间：拒绝服务启动前签发的旧 token
            Date issuedAt = claims.getIssuedAt();
            if (issuedAt != null) {
                long issuedAtMillis = issuedAt.getTime();
                long startupMillis = serverStartupTracker.getStartupTimeMillis();
                if (issuedAtMillis < startupMillis) {
                    log.info("token签发时间({})早于服务启动时间({})，已失效，需重新登录", issuedAt, startupMillis);
                    response.setStatus(401);
                    return false;
                }
            }

            Long empId = Long.valueOf(claims.get(JwtClaimsConstant.EMP_ID).toString());
            log.info("当前用户id:{}", empId);
            BaseContext.setCurrentId(empId);

            // 提取角色信息
            String role = claims.get(JwtClaimsConstant.ROLE) != null
                    ? claims.get(JwtClaimsConstant.ROLE).toString()
                    : null;
            if (role != null) {
                BaseContext.setCurrentRole(role);
                log.info("当前用户角色:{}", role);
            }

            //3、通过，放行
            return true;
        } catch (Exception ex) {
            //4、不通过，响应401状态码
            log.info("jwt校验失败: {}", ex.getMessage());
            response.setStatus(401);
            return false;
        }
    }
}
