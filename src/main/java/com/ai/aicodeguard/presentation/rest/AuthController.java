package com.ai.aicodeguard.presentation.rest;

import com.ai.aicodeguard.presentation.request.auth.LoginRequest;
import com.ai.aicodeguard.presentation.request.auth.RegisterRequest;
import com.ai.aicodeguard.presentation.request.auth.UpdatePasswordRequest;
import com.ai.aicodeguard.application.service.interfaces.SysUserService;
import com.ai.aicodeguard.infrastructure.common.util.ShiroUtils;
import com.ai.aicodeguard.presentation.response.WebResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @ClassName: AuthController
 * @Description:
 * @Author: LZX
 * @Date: 2025/4/8 12:04
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private SysUserService sysUserService;

    @PostMapping("/login")
    public WebResponse<String> login(@Valid @RequestBody LoginRequest loginRequest) {
        String token = sysUserService.login(loginRequest);
        return WebResponse.success(token);
    }

    @PostMapping("/logout")
    public WebResponse<String> logout() {
        ShiroUtils.logout();
        return WebResponse.success("Logout successful");
    }

    @PostMapping("/register")
    public WebResponse<Void> register(@Valid @RequestBody RegisterRequest registerRequest) {
        sysUserService.register(registerRequest);
        return WebResponse.success();
    }

    @PostMapping("/password")
    public WebResponse<Void> updatePassword(@RequestBody UpdatePasswordRequest updatePasswordRequest) {
        String account = ShiroUtils.getUserEntity().getAccount();
        sysUserService.updatePassword(account, updatePasswordRequest);
        return WebResponse.success();
    }

    @PostMapping("/refresh")
    public WebResponse<String> refreshToken(HttpServletRequest request) {
        String oldToken = request.getHeader("Authorization");
        String newToken = sysUserService.refreshToken(oldToken);
        return WebResponse.success(newToken);
    }

    @GetMapping("/admin")
    public WebResponse<String> TestAdmin() {
        return WebResponse.success("Admin access granted");
    }
}
