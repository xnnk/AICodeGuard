package com.ai.aicodeguard.infrastructure.common.util;

import com.ai.aicodeguard.application.service.interfaces.SysUserService;
import com.ai.aicodeguard.domain.user.SysUser;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;

import java.util.Collections;
import java.util.Set;

/**
 * @ClassName: ShiroUtils
 * @Description:
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
     * @return
     */
    public static SysUser getUserEntity() {
        return (SysUser)SecurityUtils.getSubject().getPrincipal();
    }

    /**
     * 获取当前用户id
     * @return
     */
    public static Integer getUserId() {
        return getUserEntity().getId();
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
