package com.ai.aicodeguard.application.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @ClassName: LoginDTO
 * @Description:
 * @Author: LZX
 * @Date: 2025/4/8 12:00
 */
@Data
public class LoginDTO {
    @NotBlank(message = "账号不能为空")
    private String account;

    @NotBlank(message = "密码不能为空")
    private String password;
}
