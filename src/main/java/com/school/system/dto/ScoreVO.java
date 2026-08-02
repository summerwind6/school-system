package com.school.system.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ScoreVO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long scoreId;
    private Long selectionId;
    private Long teacherId;
    private BigDecimal usualScore;
    private BigDecimal finalScore;
    private BigDecimal totalScore;
    private Integer status;
    private String comment;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    
    private Long studentId;
    private String studentName;
    private String studentNo;
    private Long courseId;
    private String courseName;
    private String courseCode;
    private Long deptId;
    private String deptName;
}