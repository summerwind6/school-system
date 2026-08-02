package com.school.system.service;

import com.school.system.common.PageResult;
import com.school.system.dto.ScoreDTO;
import com.school.system.dto.ScoreStatisticsVO;
import com.school.system.dto.ScoreVO;
import com.school.system.entity.Score;

import java.util.List;

public interface ScoreService {
    
    void addScore(ScoreDTO scoreDTO);
    
    void updateScore(ScoreDTO scoreDTO);
    
    void deleteScore(Long scoreId);
    
    Score getScoreById(Long scoreId);
    
    Score getScoreBySelectionId(Long selectionId);
    
    List<Score> getScoresByStudentId(Long studentId);
    
    List<Score> getScoresByCourseId(Long courseId);
    
    List<Score> getScoresByTeacherId(Long teacherId);
    
    PageResult<Score> getScorePage(int current, int size, Long studentId, Long courseId, Long teacherId);
    
    PageResult<ScoreVO> getScoreVOPage(int current, int size, Long studentId, Long courseId, Long teacherId);
    
    void calculateTotalScore(Long scoreId);
    
    void exportScoresToExcel(Long courseId);
    
    ScoreStatisticsVO getCourseStatistics(Long courseId);
}