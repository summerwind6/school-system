package com.school.system.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

@Data
public class CourseDTO {
    
    private Long courseId;
    
    @NotBlank(message = "课程名称不能为空")
    @Size(max = 100, message = "课程名称不能超过100个字符")
    private String courseName;
    
    @NotBlank(message = "课程编码不能为空")
    @Size(max = 50, message = "课程编码不能超过50个字符")
    private String courseCode;
    
    @NotNull(message = "学分不能为空")
    @DecimalMin(value = "0.1", message = "学分必须大于0")
    private BigDecimal credits;
    
    @NotNull(message = "课时不能为空")
    @Min(value = 1, message = "课时必须大于0")
    private Integer hours;
    
    @NotNull(message = "所属院系不能为空")
    private Long deptId;
    
    private Long teacherId;
    
    @Size(max = 500, message = "课程描述不能超过500个字符")
    private String description;
    
    private Integer auditStatus;
    
    private Integer status;

    @Min(value = 1, message = "课程容量必须大于0")
    private Integer capacity;
}
