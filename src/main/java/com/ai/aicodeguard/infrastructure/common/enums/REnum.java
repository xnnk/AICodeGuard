package com.ai.aicodeguard.infrastructure.common.enums;

public enum REnum {

    UNkNOWN_ACCOUNT(401,"用户不存在"),

    PARAM_ERROR(401, "参数不正确"),

    ACCOUNT_EXIST(401,"该账号已存在"),

    USERNAME_OR_PASSWORD_ERROR(401,"用户名或密码错误"),

    ACCOUNT_DISABLE(401,"账号已被禁用"),

    AUTH_ERROR(401,"账户验证失败"),

    NOT_LOGIN(401,"未登录"),

    NOT_PERMISSION(401,"您没有访问该功能的权限"),

    USER_NOT_FOUND(401, "用户名未找到");

    private Integer code;

    private String message;

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    REnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}