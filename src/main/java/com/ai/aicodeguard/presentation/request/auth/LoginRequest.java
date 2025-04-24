package com.ai.aicodeguard.presentation.request.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @ClassName: LoginDTO
 * @Description: 登录请求DTO
 * @Author: LZX
 * @Date: 2025/4/8 12:00
 */
@Data
public class LoginRequest {
    @NotBlank(message = "账号不能为空")
    private String account;

    @NotBlank(message = "密码不能为空")
    private String password;
}
