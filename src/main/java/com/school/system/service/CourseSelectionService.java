package com.school.system.service;

import com.school.system.common.PageResult;
import com.school.system.dto.CourseSelectionDTO;
import com.school.system.dto.CourseSelectionVO;
import com.school.system.entity.CourseSelection;

import java.util.List;

public interface CourseSelectionService {
    
    void selectCourse(CourseSelectionDTO courseSelectionDTO);
    
    void dropCourse(Long selectionId);
    
    CourseSelection getSelectionById(Long selectionId);
    
    List<CourseSelection> getSelectionsByStudentId(Long studentId);
    
    List<CourseSelectionVO> getSelectionVOsByStudentId(Long studentId);
    
    List<CourseSelection> getSelectionsByCourseId(Long courseId);
    
    PageResult<CourseSelection> getSelectionPage(int current, int size, Long studentId, Long courseId);
    
    boolean checkConflict(Long studentId, Long courseId, String semester, String year);
}