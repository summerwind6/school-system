package com.school.system.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class Student implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long studentId;
    private Long userId;
    private Long deptId;
    private String studentNo;
    private String grade;
    private String className;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}