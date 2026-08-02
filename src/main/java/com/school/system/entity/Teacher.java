package com.school.system.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class Teacher implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long teacherId;
    private Long userId;
    private Long deptId;
    private String title;
    private String specialty;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}