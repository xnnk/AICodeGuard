package com.ai.aicodeguard.application.service.interfaces;

import com.ai.aicodeguard.presentation.request.auth.LoginRequest;
import com.ai.aicodeguard.presentation.request.auth.RegisterRequest;
import com.ai.aicodeguard.presentation.request.auth.UpdatePasswordRequest;
import com.ai.aicodeguard.domain.user.SysUser;

import java.util.List;
import java.util.Set;

/**
 * @InterfaceName: SysUserService
 * @Description:
 * @Author: LZX
 * @Date: 2025/4/8 12:01
 */
public interface SysUserService {
    String login(LoginRequest loginRequest);
    SysUser findByAccount(String account);
    void register(RegisterRequest registerRequest);
    void updatePassword(String account, UpdatePasswordRequest updatePasswordRequest);
    String refreshToken(String oldToken);
    Set<String> getUserPermissions(Integer userId);
    Set<String> getUserRoles(Integer userId);

    List<SysUser> list();
    void updateUser(SysUser user);
    void deleteUser(Integer id);
}
