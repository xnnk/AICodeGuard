package com.ai.aicodeguard.application.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @ClassName: RegisterDTO
 * @Description:
 * @Author: LZX
 * @Date: 2025/4/8 18:28
 */
@Data
public class RegisterDTO {
    @NotBlank(message = "账号不能为空")
    private String account;

    @NotBlank(message = "密码不能为空")
    private String password;

    @NotBlank(message = "姓名不能为空")
    private String name;
}
