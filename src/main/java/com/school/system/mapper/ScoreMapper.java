package com.school.system.mapper;

import com.school.system.dto.ScoreVO;
import com.school.system.entity.Score;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ScoreMapper {
    
    int insert(Score score);
    
    int update(Score score);
    
    int deleteById(Long scoreId);
    
    Score selectById(Long scoreId);
    
    Score selectBySelectionId(Long selectionId);
    
    List<Score> selectByStudentId(Long studentId);
    
    List<Score> selectByCourseId(Long courseId);
    
    List<Score> selectByTeacherId(Long teacherId);
    
    List<Score> selectAll();
    
    List<Score> selectPage(@Param("offset") int offset, @Param("limit") int limit, @Param("studentId") Long studentId, @Param("courseId") Long courseId, @Param("teacherId") Long teacherId);
    
    int countTotal(@Param("studentId") Long studentId, @Param("courseId") Long courseId, @Param("teacherId") Long teacherId);
    
    List<ScoreVO> selectScoreVOPage(@Param("offset") int offset, @Param("limit") int limit, @Param("studentId") Long studentId, @Param("courseId") Long courseId, @Param("teacherId") Long teacherId);
    
    int countScoreVOTotal(@Param("studentId") Long studentId, @Param("courseId") Long courseId, @Param("teacherId") Long teacherId);
}