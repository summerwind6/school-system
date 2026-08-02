package com.school.system.controller;

import com.school.system.common.PageResult;
import com.school.system.common.Result;
import com.school.system.dto.ScoreDTO;
import com.school.system.dto.ScoreStatisticsVO;
import com.school.system.dto.ScoreVO;
import com.school.system.entity.Score;
import com.school.system.service.ScoreService;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/score")
public class ScoreController {
    
    @Autowired
    private ScoreService scoreService;
    
    @PostMapping("/add")
    @RequiresRoles(logical = Logical.OR, value = {"TEACHER", "ADMIN"})
    public Result<String> addScore(@Validated @RequestBody ScoreDTO scoreDTO) {
        try {
            scoreService.addScore(scoreDTO);
            return Result.success("成绩添加成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    @PostMapping("/update")
    @RequiresRoles(logical = Logical.OR, value = {"TEACHER", "ADMIN"})
    public Result<String> updateScore(@Validated @RequestBody ScoreDTO scoreDTO) {
        try {
            scoreService.updateScore(scoreDTO);
            return Result.success("成绩更新成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    @DeleteMapping("/delete/{scoreId}")
    @RequiresRoles(logical = Logical.OR, value = {"TEACHER", "ADMIN"})
    public Result<String> deleteScore(@PathVariable Long scoreId) {
        try {
            scoreService.deleteScore(scoreId);
            return Result.success("成绩删除成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    @GetMapping("/detail/{scoreId}")
    public Result<Score> getScoreById(@PathVariable Long scoreId) {
        try {
            Score score = scoreService.getScoreById(scoreId);
            return Result.success(score);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    @GetMapping("/selection/{selectionId}")
    public Result<Score> getScoreBySelectionId(@PathVariable Long selectionId) {
        try {
            Score score = scoreService.getScoreBySelectionId(selectionId);
            return Result.success(score);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    @GetMapping("/student/{studentId}")
    @RequiresRoles(value = {"STUDENT", "TEACHER", "ADMIN"}, logical = Logical.OR)
    public Result<List<Score>> getScoresByStudentId(@PathVariable Long studentId) {
        try {
            List<Score> scores = scoreService.getScoresByStudentId(studentId);
            return Result.success(scores);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    @GetMapping("/course/{courseId}")
    @RequiresRoles(value = {"STUDENT", "TEACHER", "ADMIN"}, logical = Logical.OR)
    public Result<List<Score>> getScoresByCourseId(@PathVariable Long courseId) {
        try {
            List<Score> scores = scoreService.getScoresByCourseId(courseId);
            return Result.success(scores);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    @GetMapping("/teacher/{teacherId}")
    @RequiresRoles(value = {"STUDENT", "TEACHER", "ADMIN"}, logical = Logical.OR)
    public Result<List<Score>> getScoresByTeacherId(@PathVariable Long teacherId) {
        try {
            List<Score> scores = scoreService.getScoresByTeacherId(teacherId);
            return Result.success(scores);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    @GetMapping("/list")
    @RequiresAuthentication
    public Result<PageResult<ScoreVO>> getScorePage(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Long teacherId) {
        try {
            PageResult<ScoreVO> pageResult = scoreService.getScoreVOPage(current, size, studentId, courseId, teacherId);
            return Result.success(pageResult);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    @PostMapping("/calculate/{scoreId}")
    @RequiresRoles(logical = Logical.OR, value = {"TEACHER", "ADMIN"})
    public Result<String> calculateTotalScore(@PathVariable Long scoreId) {
        try {
            scoreService.calculateTotalScore(scoreId);
            return Result.success("总评成绩计算成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    @GetMapping("/export/{courseId}")
    @RequiresRoles(logical = Logical.OR, value = {"TEACHER", "ADMIN"})
    public Result<String> exportScoresToExcel(@PathVariable Long courseId) {
        try {
            scoreService.exportScoresToExcel(courseId);
            return Result.success("成绩导出成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    @GetMapping("/statistics/{courseId}")
    @RequiresRoles(value = {"TEACHER", "ADMIN"}, logical = Logical.OR)
    public Result<ScoreStatisticsVO> getCourseStatistics(@PathVariable Long courseId) {
        try {
            ScoreStatisticsVO statistics = scoreService.getCourseStatistics(courseId);
            return Result.success(statistics);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}