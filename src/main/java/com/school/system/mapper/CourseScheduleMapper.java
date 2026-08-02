package com.school.system.mapper;

import com.school.system.entity.CourseSchedule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CourseScheduleMapper {
    List<CourseSchedule> selectByCourseId(@Param("courseId") Long courseId);
}
