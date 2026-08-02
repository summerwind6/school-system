package com.school.system.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class SysPermission implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long permissionId;
    private String permissionName;
    private String permissionCode;
    private String url;
    private String method;
    private Long parentId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}