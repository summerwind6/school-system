package com.school.system.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class Department implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long deptId;
    private String deptName;
    private String deptCode;
    private String description;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}