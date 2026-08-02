package com.school.system.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.NotBlank;

@Data
public class CourseSelectionDTO {
    
    private Long selectionId;
    
    /**
     * 兼容旧客户端的冗余字段；服务端永远以当前会话对应的学生身份为准。
     */
    private Long studentId;
    
    @NotNull(message = "课程ID不能为空")
    private Long courseId;
    
    @NotBlank(message = "学期不能为空")
    private String semester;
    
    @NotBlank(message = "学年不能为空")
    private String year;
    
    private Integer status;
}
