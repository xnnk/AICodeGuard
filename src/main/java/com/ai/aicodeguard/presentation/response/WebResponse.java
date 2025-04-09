package com.ai.aicodeguard.presentation.response;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * @ClassName: WebResponse
 * @Description:
 * @Author: LZX
 * @Date: 2025/4/7 13:53
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WebResponse<T>(Integer status, String message, T data) {
    private static final int SUCCESS_CODE = 200;

    private static final int ERROR_CODE = 500;

    public Integer getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    // 用于链式调用的 with 方法
    public WebResponse<T> withStatus(Integer status) {
        return new WebResponse<>(status, this.message, this.data);
    }

    public WebResponse<T> withMessage(String message) {
        return new WebResponse<>(this.status, message, this.data);
    }

    public WebResponse<T> withData(T data) {
        return new WebResponse<>(this.status, this.message, data);
    }

    public static WebResponse<Void> success() {
        return new WebResponse<>(SUCCESS_CODE, "", null);
    }

    public static WebResponse<Void> successWithMessage(String msg) {
        return new WebResponse<>(SUCCESS_CODE, msg, null);
    }

    public static <T> WebResponse<T> success(T data) {
        return new WebResponse<>(SUCCESS_CODE, "", data);
    }

    public static WebResponse<Void> fail(Integer status, String message) {
        return new WebResponse<>(status, message, null);
    }

    public static WebResponse<Void> fail(String message) {
        return new WebResponse<>(ERROR_CODE, message, null);
    }
}
