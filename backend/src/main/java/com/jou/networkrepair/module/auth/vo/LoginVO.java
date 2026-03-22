package com.jou.networkrepair.module.auth.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Set;

@Data
@AllArgsConstructor
public class LoginVO {
    private String token;
    private String role;
    private String username;
    private String employeeNo;
    private Set<String> roles;
    private Set<String> permissions;
}
