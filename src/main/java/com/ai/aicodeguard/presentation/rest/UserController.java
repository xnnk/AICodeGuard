package com.ai.aicodeguard.presentation.rest;

import com.ai.aicodeguard.application.service.interfaces.SysUserService;
import com.ai.aicodeguard.domain.user.SysUser;
import com.ai.aicodeguard.infrastructure.common.annotation.RequiresPermissions;
import com.ai.aicodeguard.infrastructure.common.enums.Logical;
import com.ai.aicodeguard.presentation.response.WebResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @ClassName: UserController
 * @Description:
 * @Author: LZX
 * @Date: 2025/4/9 10:52
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private SysUserService userService;

    @RequiresPermissions("sys:user:view")
    @GetMapping("/list")
    public WebResponse<List<SysUser>> list() {
        return WebResponse.success(userService.list());
    }

    @RequiresPermissions(value = {"sys:user:edit"}, logical = Logical.OR)
    @PostMapping("/update")
    public WebResponse<Void> update(@RequestBody SysUser user) {
        userService.updateUser(user);
        return WebResponse.success();
    }

    @RequiresPermissions(value = {"sys:user:delete"}, logical = Logical.OR)
    @DeleteMapping("/{id}")
    public WebResponse<Void> delete(@PathVariable Integer id) {
        userService.deleteUser(id);
        return WebResponse.success();
    }
}