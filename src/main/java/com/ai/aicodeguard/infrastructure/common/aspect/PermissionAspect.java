package com.ai.aicodeguard.infrastructure.common.aspect;

import com.ai.aicodeguard.infrastructure.common.annotation.RequiresPermissions;
import com.ai.aicodeguard.infrastructure.common.enums.Logical;
import com.ai.aicodeguard.infrastructure.common.enums.REnum;
import com.ai.aicodeguard.infrastructure.common.util.ShiroUtils;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;

/**
 * @ClassName: PermissionAspect
 * @Description: 权限切面
 * @Author: LZX
 * @Date: 2025/4/9 10:17
 */
@Aspect
@Component
public class PermissionAspect {
    @Before("@annotation(requiresPermissions)")
    public void checkPermission(RequiresPermissions requiresPermissions) {
        String[] permissions = requiresPermissions.value();
        Logical logical = requiresPermissions.logical();

        if (!hasPermission(permissions, logical)) {
            throw new RuntimeException(REnum.NOT_PERMISSION.getMessage());
        }
    }

    private boolean hasPermission(String[] permissions, Logical logical) {
        // 直接获取当前用户的权限列表
        Set<String> userPerms = ShiroUtils.getUserPermissions();

        if (Logical.AND.equals(logical)) {
            return Arrays.stream(permissions).allMatch(userPerms::contains);
        } else {
            return Arrays.stream(permissions).anyMatch(userPerms::contains);
        }
    }
}
