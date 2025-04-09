package com.ai.aicodeguard.infrastructure.common.enums;

public enum  ForbiddenEnum {

    ENABLE(0, "启用"),

    DISABLE(1, "禁用");

    private Integer code;

    private String message;

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    ForbiddenEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}