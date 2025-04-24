package com.ai.aicodeguard.infrastructure.common.security;

import com.ai.aicodeguard.application.service.interfaces.SysUserService;
import com.ai.aicodeguard.domain.user.SysUser;
import com.ai.aicodeguard.infrastructure.common.enums.REnum;
import com.ai.aicodeguard.infrastructure.common.util.JwtUtils;
import io.jsonwebtoken.Claims;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * @ClassName: JwtRealm
 * @Description:
 * @Author: LZX
 * @Date: 2025/4/8 11:51
 */
@Component
public class JwtRealm extends AuthorizingRealm {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private SysUserService sysUserService;

    public JwtRealm(JwtUtils jwtUtils, SysUserService sysUserService) {
        this.jwtUtils = jwtUtils;
        this.sysUserService = sysUserService;
    }

    @Override
    public boolean supports(AuthenticationToken token) {
        return token instanceof JwtToken;
    }

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        SysUser user = (SysUser) principals.getPrimaryPrincipal();
        SimpleAuthorizationInfo authInfo = new SimpleAuthorizationInfo();

        // 获取用户权限
        Set<String> permissions = sysUserService.getUserPermissions(user.getId());
        authInfo.setStringPermissions(permissions);

        return authInfo;
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        JwtToken jwtToken = (JwtToken) token;
        String tokenCredential = (String) jwtToken.getCredentials();
        String username = (String) jwtToken.getPrincipal();

        try {
            // 验证令牌
            if (!jwtUtils.isTokenValid(tokenCredential)) {
                throw new AuthenticationException("令牌无效或已过期");
            }

            // 获取用户信息
            SysUser user = sysUserService.findByAccount(username);
            if (user == null) {
                throw new AuthenticationException("用户不存在");
            }

            // 返回认证信息，Principal为用户对象
            return new SimpleAuthenticationInfo(user, tokenCredential, getName());
        } catch (Exception e) {
            throw new AuthenticationException(e);
        }
    }
}
