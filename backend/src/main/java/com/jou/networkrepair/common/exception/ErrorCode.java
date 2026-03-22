
package com.jou.networkrepair.common.exception;

public enum ErrorCode {
    SUCCESS(200, "success"),
    VALIDATION_ERROR(400, "参数校验失败"),
    BIZ_ERROR(500, "业务异常"),
    SYSTEM_ERROR(500, "系统异常");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() { return code; }
    public String getMessage() { return message; }
}
