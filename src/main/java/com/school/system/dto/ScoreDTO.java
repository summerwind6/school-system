package com.school.system.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import java.math.BigDecimal;

@Data
public class ScoreDTO {
    
    private Long scoreId;
    
    private Long selectionId;
    
    private Long teacherId;
    
    private Long courseId;
    
    private String studentNo;
    
    @DecimalMin(value = "0.0", message = "平时成绩不能小于0")
    @DecimalMax(value = "100.0", message = "平时成绩不能大于100")
    private BigDecimal usualScore;
    
    @DecimalMin(value = "0.0", message = "期末成绩不能小于0")
    @DecimalMax(value = "100.0", message = "期末成绩不能大于100")
    private BigDecimal finalScore;
    
    private BigDecimal totalScore;
    
    private Integer status;
    
    private String comment;
}
