package com.ai.aicodeguard.domain.user.model;

import com.ai.aicodeguard.domain.user.SysResource;
import com.ai.aicodeguard.domain.user.SysRole;
import com.ai.aicodeguard.domain.user.SysUser;

import java.util.Set;

/**
 * @ClassName: Identity
 * @Description: 聚合根
 * @Author: LZX
 * @Date: 2025/4/16 10:35
 */
public class Identity {
    private SysUser user;
    private Set<SysRole> roles;
    private Set<SysResource> resources;

    /**
     * 检查用户是否拥有资源访问权限
     * @param resource
     * @return
     */
    public boolean canAccess(String resource) {
        return resources.stream().anyMatch(r -> r.getPerms().equals(resource));
    }
}
