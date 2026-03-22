package com.jou.networkrepair.module.auth.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class ThirdPartyBindDTO {
    @NotBlank(message = "授权码不能为空")
    private String code;
}
