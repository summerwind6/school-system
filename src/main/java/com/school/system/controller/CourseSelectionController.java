package com.school.system.controller;

import com.school.system.common.PageResult;
import com.school.system.common.Result;
import com.school.system.dto.CourseSelectionDTO;
import com.school.system.dto.CourseSelectionVO;
import com.school.system.entity.CourseSelection;
import com.school.system.service.CourseSelectionService;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/selection")
public class CourseSelectionController {
    
    @Autowired
    private CourseSelectionService courseSelectionService;
    
    @PostMapping("/select")
    @RequiresRoles("STUDENT")
    public Result<String> selectCourse(@Validated @RequestBody CourseSelectionDTO courseSelectionDTO) {
        courseSelectionService.selectCourse(courseSelectionDTO);
        return Result.success("选课成功");
    }
    
    @DeleteMapping("/drop/{selectionId}")
    @RequiresRoles("STUDENT")
    public Result<String> dropCourse(@PathVariable Long selectionId) {
        courseSelectionService.dropCourse(selectionId);
        return Result.success("退课成功");
    }
    
    @GetMapping("/detail/{selectionId}")
    @RequiresRoles("STUDENT")
    public Result<CourseSelection> getSelectionById(@PathVariable Long selectionId) {
        return Result.success(courseSelectionService.getSelectionById(selectionId));
    }
    
    @GetMapping("/student/{studentId}")
    @RequiresRoles("STUDENT")
    public Result<List<CourseSelectionVO>> getSelectionsByStudentId(@PathVariable Long studentId) {
        return Result.success(courseSelectionService.getSelectionVOsByStudentId(studentId));
    }
    
    @GetMapping("/course/{courseId}")
    @RequiresRoles(value = {"TEACHER", "ADMIN"}, logical = org.apache.shiro.authz.annotation.Logical.OR)
    public Result<List<CourseSelection>> getSelectionsByCourseId(@PathVariable Long courseId) {
        return Result.success(courseSelectionService.getSelectionsByCourseId(courseId));
    }
    
    @GetMapping("/list")
    @RequiresRoles(value = {"STUDENT", "TEACHER", "ADMIN"}, logical = org.apache.shiro.authz.annotation.Logical.OR)
    public Result<PageResult<CourseSelection>> getSelectionPage(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long courseId) {
        return Result.success(courseSelectionService.getSelectionPage(current, size, studentId, courseId));
    }
    
    @GetMapping("/check")
    @RequiresRoles("STUDENT")
    public Result<Boolean> checkConflict(
            @RequestParam Long courseId,
            @RequestParam String semester,
            @RequestParam String year) {
        return Result.success(courseSelectionService.checkConflict(null, courseId, semester, year));
    }
}
