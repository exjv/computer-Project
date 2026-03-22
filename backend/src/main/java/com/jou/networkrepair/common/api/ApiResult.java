package com.jou.networkrepair.common.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResult<T> {
    private Integer code;
    private String message;
    private T data;

    public static <T> ApiResult<T> success(T data) { return new ApiResult<>(200, "success", data); }
    public static <T> ApiResult<T> success(String message, T data) { return new ApiResult<>(200, message, data); }
    public static <T> ApiResult<T> fail(Integer code, String message) { return new ApiResult<>(code, message, null); }
    public static <T> ApiResult<T> fail(String message) { return new ApiResult<>(500, message, null); }
}
