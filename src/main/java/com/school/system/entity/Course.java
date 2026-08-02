package com.school.system.entity;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Course implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long courseId;
    private String courseName;
    private String courseCode;
    private BigDecimal credits;
    private Integer hours;
    private Long deptId;
    private Long teacherId;
    private String description;
    private Integer auditStatus;
    private Integer status;
    private Integer capacity;
    private Integer selectedCount;
    private Integer version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
