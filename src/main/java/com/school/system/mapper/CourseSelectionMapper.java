package com.school.system.mapper;

import com.school.system.entity.CourseSelection;
import com.school.system.dto.CourseSelectionVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CourseSelectionMapper {
    
    int insert(CourseSelection courseSelection);
    
    int update(CourseSelection courseSelection);
    
    int deleteById(Long selectionId);
    
    CourseSelection selectById(Long selectionId);
    
    List<CourseSelection> selectByStudentId(Long studentId);

    List<CourseSelection> selectActiveByStudentId(Long studentId);
    
    List<CourseSelection> selectByCourseId(Long courseId);
    
    CourseSelection selectByStudentAndCourse(@Param("studentId") Long studentId, @Param("courseId") Long courseId, @Param("semester") String semester, @Param("year") String year);
    
    List<CourseSelection> selectAll();
    
    List<CourseSelection> selectPage(@Param("offset") int offset, @Param("limit") int limit, @Param("studentId") Long studentId, @Param("courseId") Long courseId);
    
    int countTotal(@Param("studentId") Long studentId, @Param("courseId") Long courseId);
    
    int countByCourseIdAndStatus(@Param("courseId") Long courseId, @Param("status") Integer status);

    int cancelIfActive(@Param("selectionId") Long selectionId);

    int reactivate(@Param("selectionId") Long selectionId);

    List<CourseSelectionVO> selectVOByStudentId(@Param("studentId") Long studentId);
}
