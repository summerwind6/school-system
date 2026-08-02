package com.school.system.service;

import com.school.system.common.PageResult;
import com.school.system.dto.CourseDTO;
import com.school.system.entity.Course;

import java.util.List;

public interface CourseService {
    
    void addCourse(CourseDTO courseDTO);
    
    void updateCourse(CourseDTO courseDTO);
    
    void deleteCourse(Long courseId);
    
    Course getCourseById(Long courseId);
    
    List<Course> getAllCourses();
    
    PageResult<Course> getCoursePage(int current, int size, String courseName);
    
    void approveCourse(Long courseId);
    
    void rejectCourse(Long courseId);
    
    void updateCourseStatus(Long courseId, Integer status);
    
    List<Course> getPendingCourses();
    
    List<Course> getApprovedCourses();
    
    PageResult<Course> getPendingCoursePage(int current, int size, String courseName);
}