package com.school.system.mapper;

import com.school.system.dto.SelectableCourse;
import com.school.system.entity.Course;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface CourseMapper {
    
    int insert(Course course);
    
    int update(Course course);

    int updateAudit(@Param("courseId") Long courseId, @Param("auditStatus") int auditStatus);

    int updateStatus(@Param("courseId") Long courseId, @Param("status") int status);
    
    int deleteById(Long courseId);
    
    Course selectById(Long courseId);
    
    Course selectByCourseCode(String courseCode);
    
    List<Course> selectByTeacherId(Long teacherId);
    
    List<Course> selectByDeptId(Long deptId);
    
    List<Course> selectAll();
    
    List<Course> selectByAuditStatus(Integer auditStatus);
    
    List<Course> selectApproved();
    
    List<Course> selectPage(@Param("offset") int offset, @Param("limit") int limit, @Param("courseName") String courseName);
    
    int countTotal(@Param("courseName") String courseName);

    List<SelectableCourse> selectSelectable(@Param("offset") long offset, @Param("limit") int limit,
            @Param("courseName") String courseName, @Param("deptId") Long deptId,
            @Param("minCredits") BigDecimal minCredits, @Param("maxCredits") BigDecimal maxCredits);

    long countSelectable(@Param("courseName") String courseName, @Param("deptId") Long deptId,
            @Param("minCredits") BigDecimal minCredits, @Param("maxCredits") BigDecimal maxCredits);

    int reserveSeat(@Param("courseId") Long courseId);

    int releaseSeat(@Param("courseId") Long courseId);
}
