package com.ai.aicodeguard.infrastructure.common.security;

import com.ai.aicodeguard.application.service.interfaces.SysUserService;
import com.ai.aicodeguard.domain.user.SysUser;
import com.ai.aicodeguard.infrastructure.common.util.JwtUtils;
import com.ai.aicodeguard.infrastructure.common.util.SpringContextUtils;
import com.ai.aicodeguard.presentation.response.WebResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.web.filter.authc.BasicHttpAuthenticationFilter;
import org.springframework.http.HttpStatus;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @ClassName: JwtFilter
 * @Description: JWT过滤器
 * @Author: LZX
 * @Date: 2025/4/8 11:50
 */
@Slf4j
public class JwtFilter extends BasicHttpAuthenticationFilter {

    @Override
    protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue) {
        try {
            return executeLogin(request, response);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    protected boolean executeLogin(ServletRequest request, ServletResponse response) {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        String token = httpServletRequest.getHeader("Authorization");

        if (StringUtils.isBlank(token)) {
            return false;
        }

        try {
            // 验证token
            JwtUtils jwtUtils = SpringContextUtils.getBean(JwtUtils.class);
            if (!jwtUtils.isTokenValid(token)) {
                return false;
            }

            // 从token中获取用户名
            Claims claims = jwtUtils.getClaimsFromToken(token);
            String username = claims.getSubject();

            // 获取用户对象
            SysUserService userService = SpringContextUtils.getBean(SysUserService.class);
            SysUser user = userService.findByAccount(username);
            if (user == null) {
                return false;
            }

            // 创建JWT令牌并执行登录
            JwtToken jwtToken = new JwtToken(user, token);
            SecurityUtils.getSubject().login(jwtToken);

            // 记录认证成功信息
            httpServletRequest.setAttribute("currentUser", user);

            return true;
        } catch (Exception e) {
            log.error("Token验证失败", e);
            return false;
        }
    }

    @Override
    protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws IOException {
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        httpResponse.setStatus(HttpStatus.UNAUTHORIZED.value());
        httpResponse.setContentType("application/json;charset=UTF-8");
        httpResponse.getWriter().write(new ObjectMapper().writeValueAsString(
                WebResponse.fail("未授权")
        ));
        return false;
    }
}
