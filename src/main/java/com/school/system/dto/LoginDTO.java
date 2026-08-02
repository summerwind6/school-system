package com.school.system.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class LoginDTO {
    
    @NotBlank(message = "学号/工号不能为空")
    private String username;
    
    @NotBlank(message = "密码不能为空")
    private String password;
}