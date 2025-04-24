package com.ai.aicodeguard.infrastructure.common.util;

import com.ai.aicodeguard.application.service.interfaces.SysUserService;
import com.ai.aicodeguard.domain.user.SysUser;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;
import java.util.Set;

/**
 * @ClassName: ShiroUtils
 * @Description: Shiro工具类
 * @Author: LZX
 * @Date: 2025/4/8 18:33
 */
public class ShiroUtils {

    /**
     * 获取当前交互对象
     * @return
     */
    public static Subject getSubject() {
        return SecurityUtils.getSubject();
    }

    /**
     * 获取当前用户对象
     * @return 当前用户对象，未登录返回null
     */
    public static SysUser getUserEntity() {
        try {
            Subject subject = SecurityUtils.getSubject();
            Object principal = subject.getPrincipal();

            // 如果已有用户对象，直接返回
            if (principal instanceof SysUser) {
                return (SysUser) principal;
            }

            // 尝试从请求头获取JWT token
            HttpServletRequest request = getRequest();
            if (request != null) {
                String token = request.getHeader("Authorization");
                if (token != null && !token.isEmpty()) {
                    JwtUtils jwtUtils = SpringContextUtils.getBean(JwtUtils.class);
                    if (jwtUtils.isTokenValid(token)) {
                        Claims claims = jwtUtils.getClaimsFromToken(token);
                        String username = claims.getSubject();

                        // 从数据库获取用户信息
                        SysUserService userService = SpringContextUtils.getBean(SysUserService.class);
                        return userService.findByAccount(username);
                    }
                }
            }

            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取HttpServletRequest
     */
    private static HttpServletRequest getRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes)
                    RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取当前用户ID
     * @return 用户ID，未登录返回null
     */
    public static Integer getUserId() {
        SysUser user = getUserEntity();
        return user != null ? user.getId() : null;
    }

    /**
     * 获取当前会话
     * @return
     */
    public static Session getSession() {
        return SecurityUtils.getSubject().getSession();
    }

    /**
     * 设置Session
     * @param key
     * @param value
     */
    public static void setSessionAttribute(Object key, Object value) {
        getSession().setAttribute(key, value);
    }

    /**
     * 获取session信息
     * @param key
     * @return
     */
    public static Object getSessionAttribute(Object key) {
        return getSession().getAttribute(key);
    }

    /**
     * 判断当前用户是否登录
     * @return
     */
    public static boolean isLogin() {
        return SecurityUtils.getSubject().getPrincipal() != null;
    }

    /**
     * 退出
     */
    public static void logout() {
        SecurityUtils.getSubject().logout();
    }

    public static Set<String> getUserPermissions() {
        SysUser user = getUserEntity();
        if (user == null) {
            return Collections.emptySet();
        }
        return SpringContextUtils.getBean(SysUserService.class)
                .getUserPermissions(user.getId());
    }

    public static Set<String> getUserRoles() {
        SysUser user = getUserEntity();
        if (user == null) {
            return Collections.emptySet();
        }
        return SpringContextUtils.getBean(SysUserService.class)
                .getUserRoles(user.getId());
    }
}
