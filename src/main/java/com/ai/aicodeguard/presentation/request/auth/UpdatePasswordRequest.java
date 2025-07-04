package com.ai.aicodeguard.presentation.request.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @ClassName: UpdatePasswordDTO
 * @Description: 更新密码请求DTO
 * @Author: LZX
 * @Date: 2025/4/8 18:28
 */
@Data
public class UpdatePasswordRequest {
    @NotBlank(message = "原密码不能为空")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    private String newPassword;
}
