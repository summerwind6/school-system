package com.school.system.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class CourseSelection implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long selectionId;
    private Long studentId;
    private Long courseId;
    private String semester;
    private String year;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}