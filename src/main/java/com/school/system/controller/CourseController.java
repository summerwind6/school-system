package com.school.system.controller;

import com.school.system.common.PageResult;
import com.school.system.common.Result;
import com.school.system.dto.CourseDTO;
import com.school.system.entity.Course;
import com.school.system.service.CourseService;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/course")
public class CourseController {
    
    @Autowired
    private CourseService courseService;
    
    @PostMapping("/add")
    @RequiresRoles(logical = Logical.OR, value = {"TEACHER", "ADMIN"})
    public Result<String> addCourse(@Validated @RequestBody CourseDTO courseDTO) {
        try {
            courseService.addCourse(courseDTO);
            return Result.success("课程添加成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    @PostMapping("/update")
    @RequiresRoles(logical = Logical.OR, value = {"TEACHER", "ADMIN"})
    public Result<String> updateCourse(@Validated @RequestBody CourseDTO courseDTO) {
        try {
            courseService.updateCourse(courseDTO);
            return Result.success("课程更新成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    @DeleteMapping("/delete/{courseId}")
    @RequiresRoles(logical = Logical.OR, value = {"TEACHER", "ADMIN"})
    public Result<String> deleteCourse(@PathVariable Long courseId) {
        try {
            courseService.deleteCourse(courseId);
            return Result.success("课程删除成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    @GetMapping("/detail/{courseId}")
    public Result<Course> getCourseById(@PathVariable Long courseId) {
        try {
            Course course = courseService.getCourseById(courseId);
            return Result.success(course);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    @GetMapping("/list")
    public Result<PageResult<Course>> getCoursePage(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String courseName) {
        try {
            PageResult<Course> pageResult = courseService.getCoursePage(current, size, courseName);
            return Result.success(pageResult);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    @GetMapping("/all")
    public Result<java.util.List<Course>> getAllCourses() {
        try {
            java.util.List<Course> courses = courseService.getAllCourses();
            return Result.success(courses);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    @PostMapping("/approve/{courseId}")
    @RequiresRoles("ADMIN")
    public Result<String> approveCourse(@PathVariable Long courseId) {
        try {
            courseService.approveCourse(courseId);
            return Result.success("课程审核通过");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    @PostMapping("/reject/{courseId}")
    @RequiresRoles("ADMIN")
    public Result<String> rejectCourse(@PathVariable Long courseId) {
        try {
            courseService.rejectCourse(courseId);
            return Result.success("课程已拒绝");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    @PostMapping("/status/{courseId}")
    @RequiresRoles("ADMIN")
    public Result<String> updateCourseStatus(@PathVariable Long courseId, @RequestParam Integer status) {
        try {
            courseService.updateCourseStatus(courseId, status);
            return Result.success("课程状态更新成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    @GetMapping("/pending")
    @RequiresRoles("ADMIN")
    public Result<PageResult<Course>> getPendingCourses(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String courseName) {
        try {
            PageResult<Course> pageResult = courseService.getPendingCoursePage(current, size, courseName);
            return Result.success(pageResult);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    @GetMapping("/approved")
    public Result<java.util.List<Course>> getApprovedCourses() {
        try {
            java.util.List<Course> courses = courseService.getApprovedCourses();
            return Result.success(courses);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}