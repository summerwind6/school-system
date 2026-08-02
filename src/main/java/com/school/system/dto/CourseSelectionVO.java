package com.school.system.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CourseSelectionVO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    // 选课信息
    private Long selectionId;
    private Long studentId;
    private Long courseId;
    private String semester;
    private String year;
    private Integer status;
    private LocalDateTime createTime;
    
    // 课程信息（关联查询）
    private String courseName;
    private String courseCode;
    private BigDecimal credits;
    private Integer hours;
    private Long deptId;
    private Long teacherId;
    private String description;
    private Integer courseStatus;
    
    // 教师信息（可选）
    private String teacherName;
}
