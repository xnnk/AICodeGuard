package com.ai.aicodeguard.application.service.impl;

import com.ai.aicodeguard.infrastructure.common.security.JwtToken;
import com.ai.aicodeguard.presentation.request.auth.LoginRequest;
import com.ai.aicodeguard.presentation.request.auth.RegisterRequest;
import com.ai.aicodeguard.presentation.request.auth.UpdatePasswordRequest;
import com.ai.aicodeguard.application.service.interfaces.SysUserService;
import com.ai.aicodeguard.domain.user.*;
import com.ai.aicodeguard.infrastructure.common.enums.REnum;
import com.ai.aicodeguard.infrastructure.common.util.JwtUtils;
import com.ai.aicodeguard.infrastructure.persistence.*;
import io.jsonwebtoken.Claims;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.crypto.hash.Sha256Hash;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @ClassName: SysUserServiceImpl
 * @Description:
 * @Author: LZX
 * @Date: 2025/4/8 12:02
 */
@Service
public class SysUserServiceImpl implements SysUserService {

    @Autowired
    private SysUserRepository sysUserRepository;

    @Autowired
    private SysResourceRepository resourceRepository;

    @Autowired
    private SysRoleRepository roleRepository;

    @Autowired
    private SysUserRoleRepository userRoleRepository;

    @Autowired
    private SysRoleResourceRepository roleResourceRepository;

    @Autowired
    private JwtUtils jwtUtils;

    @Override
    public String login(LoginRequest loginRequest) {
        if (loginRequest == null || StringUtils.isBlank(loginRequest.getAccount())) {
            throw new RuntimeException(REnum.PARAM_ERROR.getMessage());
        }

        // 查询用户及其角色信息
        SysUser user = sysUserRepository.findByAccountWithRole(loginRequest.getAccount())
                .orElseThrow(() -> new RuntimeException(REnum.UNkNOWN_ACCOUNT.getMessage()));

        // 验证密码
        String encryptedPassword = new Sha256Hash(loginRequest.getPassword(), user.getSalt()).toHex();
        if (!user.getPassword().equals(encryptedPassword)) {
            throw new RuntimeException(REnum.USERNAME_OR_PASSWORD_ERROR.getMessage());
        }

        // 检查用户状态
        if (user.checkEnabled()) {
            throw new RuntimeException(REnum.ACCOUNT_DISABLE.getMessage());
        }

        // 获取用户权限
        Set<String> permissions = getUserPermissions(user.getId());
        Set<String> roles = getUserRoles(user.getId());

        // 生成包含权限信息的token
        String token = jwtUtils.generateTokenWithClaims(user.getAccount(), Map.of(
                "permissions", String.join(",", permissions),
                "roles", String.join(",", roles)
        ));

        // 创建JwtToken并登录
        JwtToken jwtToken = new JwtToken(user.getAccount(), token);
        try {
            // 执行登录
            SecurityUtils.getSubject().login(jwtToken);
            return token;
        } catch (Exception e) {
            throw new RuntimeException("登录失败: " + e.getMessage());
        }
    }

    @Override
    public SysUser findByAccount(String account) {
        return sysUserRepository.findByAccountWithRole(account)
            .orElseThrow(() -> new RuntimeException(REnum.UNkNOWN_ACCOUNT.getMessage()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterRequest registerRequest) {
        // 检查账号是否存在
        if(sysUserRepository.findByAccount(registerRequest.getAccount()).isPresent()) {
            throw new RuntimeException(REnum.ACCOUNT_EXIST.getMessage());
        }

        // 创建用户
        SysUser user = new SysUser();
        user.setAccount(registerRequest.getAccount());
        user.setName(registerRequest.getName());
        user.setForbidden("0");

        // 生成盐值和加密密码
        String salt = UUID.randomUUID().toString().replaceAll("-", "");
        user.setSalt(salt);
        String encryptedPassword = new Sha256Hash(registerRequest.getPassword(), salt).toHex();
        user.setPassword(encryptedPassword);

        // 保存用户
        sysUserRepository.save(user);

        // 分配默认角色
        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(2); // 默认普通用户角色ID
        userRoleRepository.save(userRole);
    }

    @Override
    public void updatePassword(String account, UpdatePasswordRequest dto) {
        SysUser user = findByAccount(account);

        // 验证原密码
        String oldEncryptedPassword = new Sha256Hash(dto.getOldPassword(), user.getSalt()).toHex();
        if(!user.getPassword().equals(oldEncryptedPassword)) {
            throw new RuntimeException(REnum.USERNAME_OR_PASSWORD_ERROR.getMessage());
        }

        // 更新新密码
        String newEncryptedPassword = new Sha256Hash(dto.getNewPassword(), user.getSalt()).toHex();
        user.setPassword(newEncryptedPassword);

        sysUserRepository.save(user);
    }

    @Override
    public String refreshToken(String oldToken) {
        if(!jwtUtils.isTokenValid(oldToken)) {
            throw new RuntimeException(REnum.AUTH_ERROR.getMessage());
        }

        Claims claims = jwtUtils.getClaimsFromToken(oldToken);
        String username = claims.getSubject();

        // 重新获取用户权限
        SysUser user = findByAccount(username);
        Set<String> permissions = getUserPermissions(user.getId());
        Set<String> roles = getUserRoles(user.getId());

        // 生成新token，包含最新的权限信息
        return jwtUtils.generateTokenWithClaims(username, Map.of(
            "permissions", String.join(",", permissions),
            "roles", String.join(",", roles)
        ));
    }

    @Override
    public Set<String> getUserPermissions(Integer userId) {
        // 获取用户的所有角色ID
        Set<Integer> roleIds = userRoleRepository.findByUserId(userId)
            .stream()
            .map(SysUserRole::getRoleId)
            .collect(Collectors.toSet());

        if (roleIds.isEmpty()) {
            return Collections.emptySet();
        }

        // 获取所有角色的权限
        Set<String> permissions = new HashSet<>();
        for (Integer roleId : roleIds) {
            // 通过角色资源关系表获取资源权限
            List<SysRoleResource> roleResources = roleResourceRepository.findByRoleId(roleId);
            if (!roleResources.isEmpty()) {
                List<SysResource> resources = resourceRepository.findAllById(
                    roleResources.stream()
                        .map(SysRoleResource::getResourceId)
                        .collect(Collectors.toList())
                );

                permissions.addAll(resources.stream()
                    .map(SysResource::getPerms)
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toSet()));
            }
        }

        return permissions;
    }

    @Override
    public Set<String> getUserRoles(Integer userId) {
        return roleRepository.findRolesByUserId(userId).stream()
                .map(SysRole::getName)
                .collect(Collectors.toSet());
    }

    @Override
    public List<SysUser> list() {
        // 获取所有用户列表，并排除敏感信息
        return sysUserRepository.findAll().stream()
                .peek(user -> {
                    user.setPassword(null);
                    user.setSalt(null);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(SysUser user) {
        // 验证用户是否存在
        SysUser existingUser = sysUserRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException(REnum.USER_NOT_FOUND.getMessage()));

        // 防止修改敏感信息
        user.setPassword(existingUser.getPassword());
        user.setSalt(existingUser.getSalt());

        // 如果修改了账号，需要验证账号唯一性
        if (!existingUser.getAccount().equals(user.getAccount())) {
            sysUserRepository.findByAccount(user.getAccount())
                    .ifPresent(u -> {
                        throw new RuntimeException(REnum.ACCOUNT_EXIST.getMessage());
                    });
        }

        sysUserRepository.save(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Integer id) {
        // 验证用户是否存在
        SysUser user = sysUserRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(REnum.USER_NOT_FOUND.getMessage()));

        // 删除用户角色关系
        userRoleRepository.deleteByUserId(id);

        // 删除用户
        sysUserRepository.deleteById(id);
    }
}
