package com.school.system.mapper;

import com.school.system.entity.Teacher;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TeacherMapper {
    
    int insert(Teacher teacher);
    
    int update(Teacher teacher);
    
    int deleteById(Long teacherId);
    
    Teacher selectById(Long teacherId);
    
    Teacher selectByUserId(Long userId);
    
    List<Teacher> selectByDeptId(Long deptId);
    
    List<Teacher> selectAll();
}