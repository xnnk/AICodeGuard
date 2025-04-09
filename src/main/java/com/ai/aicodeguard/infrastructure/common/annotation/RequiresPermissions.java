package com.ai.aicodeguard.infrastructure.common.annotation;

import com.ai.aicodeguard.infrastructure.common.enums.Logical;

import java.lang.annotation.*;

/**
 * @ClassName: RequiresPermissions
 * @Description: 权限注解
 * @Author: LZX
 * @Date: 2025/4/9 10:13
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresPermissions {
    String[] value();
    Logical logical() default Logical.AND;
}
