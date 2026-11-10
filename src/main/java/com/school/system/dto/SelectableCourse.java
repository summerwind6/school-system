package com.school.system.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SelectableCourse {
    private Long courseId;
    private String courseName;
    private String courseCode;
    private BigDecimal credits;
    private Integer hours;
    private Long deptId;
    private String deptName;
    private String teacherName;
}
