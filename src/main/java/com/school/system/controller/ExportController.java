package com.school.system.controller;

import com.school.system.entity.Course;
import com.school.system.entity.CourseSelection;
import com.school.system.entity.Score;
import com.school.system.entity.Student;
import com.school.system.entity.SysUser;
import com.school.system.mapper.CourseMapper;
import com.school.system.mapper.CourseSelectionMapper;
import com.school.system.mapper.ScoreMapper;
import com.school.system.mapper.StudentMapper;
import com.school.system.util.ExcelUtil;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/export")
public class ExportController {
    
    @Autowired
    private ScoreMapper scoreMapper;
    
    @Autowired
    private CourseSelectionMapper courseSelectionMapper;
    
    @Autowired
    private CourseMapper courseMapper;
    
    @Autowired
    private StudentMapper studentMapper;
    
    @GetMapping("/score/{courseId}")
    @RequiresRoles({"TEACHER", "ADMIN"})
    public void exportScoreToExcel(@PathVariable Long courseId, HttpServletResponse response) throws IOException {
        List<Score> scores = scoreMapper.selectByCourseId(courseId);
        
        if (scores == null || scores.isEmpty()) {
            throw new RuntimeException("没有可导出的成绩数据");
        }
        
        Course course = courseMapper.selectById(courseId);
        String fileName = course != null ? course.getCourseName() + "_成绩表" : "成绩表";
        
        String[] headers = {"学号", "姓名", "课程名称", "平时成绩", "期末成绩", "总评成绩", "评语"};
        
        List<Map<String, Object>> data = new ArrayList<>();
        for (Score score : scores) {
            Map<String, Object> row = new HashMap<>();
            
            CourseSelection selection = courseSelectionMapper.selectById(score.getSelectionId());
            if (selection != null) {
                Student student = studentMapper.selectById(selection.getStudentId());
                if (student != null) {
                    row.put("学号", student.getStudentNo());
                    row.put("姓名", "");
                }
            }
            
            row.put("课程名称", course != null ? course.getCourseName() : "");
            row.put("平时成绩", score.getUsualScore() != null ? score.getUsualScore() : 0);
            row.put("期末成绩", score.getFinalScore() != null ? score.getFinalScore() : 0);
            row.put("总评成绩", score.getTotalScore() != null ? score.getTotalScore() : 0);
            row.put("评语", score.getComment() != null ? score.getComment() : "");
            
            data.add(row);
        }
        
        ExcelUtil.exportExcel(response, fileName, headers, data);
    }
    
    @GetMapping("/course")
    @RequiresRoles({"TEACHER", "ADMIN"})
    public void exportCourseToExcel(HttpServletResponse response) throws IOException {
        List<Course> courses = courseMapper.selectAll();
        
        if (courses == null || courses.isEmpty()) {
            throw new RuntimeException("没有可导出的课程数据");
        }
        
        String fileName = "课程表";
        String[] headers = {"课程ID", "课程名称", "课程编码", "学分", "课时", "状态"};
        
        List<Map<String, Object>> data = new ArrayList<>();
        for (Course course : courses) {
            Map<String, Object> row = new HashMap<>();
            row.put("课程ID", course.getCourseId());
            row.put("课程名称", course.getCourseName());
            row.put("课程编码", course.getCourseCode());
            row.put("学分", course.getCredits());
            row.put("课时", course.getHours());
            row.put("状态", course.getStatus() == 1 ? "启用" : "停用");
            data.add(row);
        }
        
        ExcelUtil.exportExcel(response, fileName, headers, data);
    }
}