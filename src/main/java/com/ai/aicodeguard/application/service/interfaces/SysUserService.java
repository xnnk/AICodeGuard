package com.ai.aicodeguard.application.service.interfaces;

import com.ai.aicodeguard.application.auth.LoginDTO;
import com.ai.aicodeguard.application.auth.RegisterDTO;
import com.ai.aicodeguard.application.auth.UpdatePasswordDTO;
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
    String login(LoginDTO loginDTO);
    SysUser findByAccount(String account);
    void register(RegisterDTO registerDTO);
    void updatePassword(String account, UpdatePasswordDTO updatePasswordDTO);
    String refreshToken(String oldToken);
    Set<String> getUserPermissions(Integer userId);
    Set<String> getUserRoles(Integer userId);

    List<SysUser> list();
    void updateUser(SysUser user);
    void deleteUser(Integer id);
}
