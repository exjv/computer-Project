package com.jou.networkrepair.module.auth.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class UpdatePasswordDTO {
    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;
    @NotBlank(message = "新密码不能为空")
    private String newPassword;
    @NotBlank(message = "确认新密码不能为空")
    private String confirmPassword;
    @NotBlank(message = "验证码不能为空")
    private String captchaCode;
    @NotBlank(message = "验证码标识不能为空")
    private String captchaKey;

    /**
     * 兼容历史字段名 confirmNewPassword
     */
    public String getConfirmNewPassword() {
        return confirmPassword;
    }

    public void setConfirmNewPassword(String confirmNewPassword) {
        this.confirmPassword = confirmNewPassword;
    }
}
