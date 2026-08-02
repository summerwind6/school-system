package com.school.system.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class RegisterDTO {
    
    @NotBlank(message = "密码不能为空")
    private String password;
    
    @NotBlank(message = "手机号不能为空")
    private String phone;
    
    @NotNull(message = "角色不能为空")
    private Integer roleType;
    
    private String studentNo;
    
    private String teacherNo;
    
    private String realName;
}