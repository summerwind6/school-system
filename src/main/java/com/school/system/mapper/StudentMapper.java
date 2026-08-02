package com.school.system.mapper;

import com.school.system.entity.Student;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StudentMapper {
    
    int insert(Student student);
    
    int update(Student student);
    
    int deleteById(Long studentId);
    
    Student selectById(Long studentId);
    
    Student selectByUserId(Long userId);
    
    Student selectByStudentNo(String studentNo);
    
    List<Student> selectByDeptId(Long deptId);
    
    List<Student> selectAll();
}