package com.ai.aicodeguard.infrastructure.common.security;

import com.ai.aicodeguard.application.service.interfaces.SysUserService;
import com.ai.aicodeguard.domain.user.SysUser;
import com.ai.aicodeguard.infrastructure.common.enums.ForbiddenEnum;
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

    @Override
    public boolean supports(AuthenticationToken token) {
        return token instanceof JwtToken;
    }

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        return new SimpleAuthorizationInfo();
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authenticationToken) throws AuthenticationException {
        String token = (String) authenticationToken.getCredentials();

        if (!jwtUtils.isTokenValid(token)) {
            throw new AuthenticationException(REnum.AUTH_ERROR.getMessage());
        }

        Claims claims = jwtUtils.getClaimsFromToken(token);
        String username = claims.getSubject();

        // 验证用户状态
        SysUser user = sysUserService.findByAccount(username);
        if (ForbiddenEnum.DISABLE.getCode().toString().equals(user.getForbidden())) {
            throw new AuthenticationException(REnum.ACCOUNT_DISABLE.getMessage());
        }

        return new SimpleAuthenticationInfo(token, token, getName());
    }
}
