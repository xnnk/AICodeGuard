package com.ai.aicodeguard.infrastructure.common.security;

import com.ai.aicodeguard.domain.user.SysUser;
import org.apache.shiro.authc.AuthenticationToken;

/**
 * @ClassName: JwtToken
 * @Description:
 * @Author: LZX
 * @Date: 2025/4/8 11:49
 */
public class JwtToken implements AuthenticationToken {
    private Object principal;
    private String token;

    public JwtToken(String username, String token) {
        this.principal = username;
        this.token = token;
    }

    public JwtToken(SysUser user, String token) {
        this.principal = user;
        this.token = token;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }

    @Override
    public Object getCredentials() {
        return token;
    }
}