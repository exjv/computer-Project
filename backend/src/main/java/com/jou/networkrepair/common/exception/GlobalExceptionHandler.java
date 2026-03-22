package com.jou.networkrepair.common.exception;

import com.jou.networkrepair.common.api.ApiResult;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public ApiResult<Void> handleBusiness(BusinessException ex) {
        return ApiResult.fail(ex.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class, ConstraintViolationException.class})
    public ApiResult<Void> handleValidation(Exception ex) {
        if (ex instanceof MethodArgumentNotValidException) {
            MethodArgumentNotValidException e = (MethodArgumentNotValidException) ex;
            String msg = e.getBindingResult().getFieldError() == null ? "参数校验失败" : e.getBindingResult().getFieldError().getDefaultMessage();
            return ApiResult.fail(400, msg);
        }
        if (ex instanceof BindException) {
            BindException e = (BindException) ex;
            String msg = e.getBindingResult().getFieldError() == null ? "参数绑定失败" : e.getBindingResult().getFieldError().getDefaultMessage();
            return ApiResult.fail(400, msg);
        }
        return ApiResult.fail(400, ex.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public ApiResult<Void> handleRuntime(RuntimeException ex) {
        return ApiResult.fail(ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ApiResult<Void> handle(Exception ex) {
        return ApiResult.fail("系统异常：" + ex.getMessage());
    }
}
