package com.school.system.entity;

import lombok.Data;

import java.io.Serializable;

/** 一条课程排课记录；时间冲突必须基于它而非课程总课时。 */
@Data
public class CourseSchedule implements Serializable {
    private Long id;
    private Long courseId;
    private Integer dayOfWeek;
    private Integer startSection;
    private Integer endSection;
    private Integer startWeek;
    private Integer endWeek;
    /** ALL、ODD、EVEN */
    private String weekType;
    private String classroom;
}
