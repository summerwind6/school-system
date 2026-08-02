package com.school.system.service.impl;

import com.school.system.common.PageResult;
import com.school.system.dto.ScoreDTO;
import com.school.system.dto.ScoreStatisticsVO;
import com.school.system.dto.ScoreVO;
import com.school.system.entity.Course;
import com.school.system.entity.Score;
import com.school.system.entity.Student;
import com.school.system.entity.CourseSelection;
import com.school.system.mapper.CourseMapper;
import com.school.system.mapper.ScoreMapper;
import com.school.system.mapper.StudentMapper;
import com.school.system.mapper.CourseSelectionMapper;
import com.school.system.mapper.TeacherMapper;
import com.school.system.service.ScoreService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import com.school.system.entity.SysUser;
import com.school.system.entity.Teacher;
import com.school.system.common.BusinessException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class ScoreServiceImpl implements ScoreService {
    
    @Autowired
    private ScoreMapper scoreMapper;
    
    @Autowired
    private CourseMapper courseMapper;
    
    @Autowired
    private StudentMapper studentMapper;
    
    @Autowired
    private CourseSelectionMapper courseSelectionMapper;
    
    @Autowired
    private TeacherMapper teacherMapper;
    
    @Override
    @Transactional
    public void addScore(ScoreDTO scoreDTO) {
        // 获取当前登录用户
        Subject subject = SecurityUtils.getSubject();
        SysUser currentUser = (SysUser) subject.getPrincipal();
        if (currentUser == null) {
            throw new RuntimeException("用户未登录");
        }
        
        // 获取当前用户的teacherId（如果是教师）
        // 后续优化：从当前用户直接获取教师信息
        
        // 确定selectionId
        Long selectionId = scoreDTO.getSelectionId();
        
        // 如果提供了courseId和studentNo，查找对应的选课记录
        if (selectionId == null && scoreDTO.getCourseId() != null && scoreDTO.getStudentNo() != null) {
            // 根据学生学号查询学生
            Student student = studentMapper.selectByStudentNo(scoreDTO.getStudentNo());
            if (student == null) {
                throw new RuntimeException("学生不存在");
            }
            
            // 查询选课记录
            List<CourseSelection> selections = courseSelectionMapper.selectByStudentId(student.getStudentId());
            CourseSelection targetSelection = selections.stream()
                .filter(selection -> selection.getCourseId().equals(scoreDTO.getCourseId()))
                .findFirst()
                .orElse(null);
                
            if (targetSelection == null) {
                throw new RuntimeException("该学生未选择此课程");
            }
            
            selectionId = targetSelection.getSelectionId();
        }
        
        if (selectionId == null) {
            throw new RuntimeException("请提供选课ID或课程和学生学号");
        }

        CourseSelection selection = courseSelectionMapper.selectById(selectionId);
        if (selection == null || selection.getStatus() != 1) {
            throw BusinessException.notFound("有效选课记录不存在");
        }
        assertCanGradeSelection(selection);
        
        // 检查是否已存在成绩记录
        Score existScore = scoreMapper.selectBySelectionId(selectionId);
        if (existScore != null) {
            throw new RuntimeException("该选课已存在成绩记录");
        }
        
        // 创建成绩记录
        Score score = new Score();
        score.setSelectionId(selectionId);
        
        // 获取teacherId
        Long teacherId = SecurityUtils.getSubject().hasRole("ADMIN") ? scoreDTO.getTeacherId() : currentTeacher().getTeacherId();
        if (teacherId == null) {
            throw BusinessException.badRequest("管理员录入成绩时必须指定评分教师");
        }
        score.setTeacherId(teacherId);
        
        score.setUsualScore(scoreDTO.getUsualScore() != null ? scoreDTO.getUsualScore() : BigDecimal.ZERO);
        score.setFinalScore(scoreDTO.getFinalScore() != null ? scoreDTO.getFinalScore() : BigDecimal.ZERO);
        score.setComment(scoreDTO.getComment());
        score.setStatus(scoreDTO.getStatus() != null ? scoreDTO.getStatus() : 1);
        
        calculateTotalScore(score);
        
        scoreMapper.insert(score);
    }
    
    @Override
    @Transactional
    public void updateScore(ScoreDTO scoreDTO) {
        Score score = scoreMapper.selectById(scoreDTO.getScoreId());
        if (score == null) {
            throw new RuntimeException("成绩记录不存在");
        }
        CourseSelection selection = courseSelectionMapper.selectById(score.getSelectionId());
        assertCanGradeSelection(selection);
        Long originalTeacherId = score.getTeacherId();
        
        BeanUtils.copyProperties(scoreDTO, score);
        score.setTeacherId(originalTeacherId);
        
        if (score.getUsualScore() == null) {
            score.setUsualScore(BigDecimal.ZERO);
        }
        if (score.getFinalScore() == null) {
            score.setFinalScore(BigDecimal.ZERO);
        }
        
        calculateTotalScore(score);
        score.setStatus(1);
        
        scoreMapper.update(score);
    }
    
    @Override
    @Transactional
    public void deleteScore(Long scoreId) {
        Score score = scoreMapper.selectById(scoreId);
        if (score == null) throw BusinessException.notFound("成绩记录不存在");
        assertCanGradeSelection(courseSelectionMapper.selectById(score.getSelectionId()));
        scoreMapper.deleteById(scoreId);
    }
    
    @Override
    public Score getScoreById(Long scoreId) {
        Score score = scoreMapper.selectById(scoreId);
        if (score == null) throw BusinessException.notFound("成绩记录不存在");
        assertCanReadSelection(courseSelectionMapper.selectById(score.getSelectionId()));
        return score;
    }
    
    @Override
    public Score getScoreBySelectionId(Long selectionId) {
        CourseSelection selection = courseSelectionMapper.selectById(selectionId);
        assertCanReadSelection(selection);
        return scoreMapper.selectBySelectionId(selectionId);
    }
    
    @Override
    public List<Score> getScoresByStudentId(Long studentId) {
        if (SecurityUtils.getSubject().hasRole("STUDENT") && !currentStudent().getStudentId().equals(studentId)) {
            throw BusinessException.forbidden("只能查看自己的成绩");
        }
        return scoreMapper.selectByStudentId(studentId);
    }
    
    @Override
    public List<Score> getScoresByCourseId(Long courseId) {
        assertCanReadCourse(courseId);
        return scoreMapper.selectByCourseId(courseId);
    }
    
    @Override
    public List<Score> getScoresByTeacherId(Long teacherId) {
        if (SecurityUtils.getSubject().hasRole("TEACHER") && !currentTeacher().getTeacherId().equals(teacherId)) {
            throw BusinessException.forbidden("只能查看自己的评分记录");
        }
        return scoreMapper.selectByTeacherId(teacherId);
    }
    
    @Override
    public PageResult<Score> getScorePage(int current, int size, Long studentId, Long courseId, Long teacherId) {
        int offset = (current - 1) * size;
        List<Score> records = scoreMapper.selectPage(offset, size, studentId, courseId, teacherId);
        int total = scoreMapper.countTotal(studentId, courseId, teacherId);
        return PageResult.of((long) total, records, (long) current, (long) size);
    }
    
    @Override
    public PageResult<ScoreVO> getScoreVOPage(int current, int size, Long studentId, Long courseId, Long teacherId) {
        if (current < 1 || size < 1 || size > 100) throw BusinessException.badRequest("分页参数不合法");
        if (SecurityUtils.getSubject().hasRole("STUDENT")) studentId = currentStudent().getStudentId();
        if (SecurityUtils.getSubject().hasRole("TEACHER")) teacherId = currentTeacher().getTeacherId();
        int offset = (current - 1) * size;
        List<ScoreVO> records = scoreMapper.selectScoreVOPage(offset, size, studentId, courseId, teacherId);
        int total = scoreMapper.countScoreVOTotal(studentId, courseId, teacherId);
        return PageResult.of((long) total, records, (long) current, (long) size);
    }
    
    @Override
    @Transactional
    public void calculateTotalScore(Long scoreId) {
        Score score = scoreMapper.selectById(scoreId);
        if (score != null) {
            assertCanGradeSelection(courseSelectionMapper.selectById(score.getSelectionId()));
            calculateTotalScore(score);
            scoreMapper.update(score);
        }
    }
    
    private void calculateTotalScore(Score score) {
        BigDecimal usualScore = score.getUsualScore() != null ? score.getUsualScore() : BigDecimal.ZERO;
        BigDecimal finalScore = score.getFinalScore() != null ? score.getFinalScore() : BigDecimal.ZERO;
        
        BigDecimal totalScore = usualScore.multiply(new BigDecimal("0.3")).add(finalScore.multiply(new BigDecimal("0.7")));
        score.setTotalScore(totalScore);
    }
    
    @Override
    public void exportScoresToExcel(Long courseId) {
        assertCanReadCourse(courseId);
        List<Score> scores = scoreMapper.selectByCourseId(courseId);
        
        if (scores == null || scores.isEmpty()) {
            throw new RuntimeException("没有可导出的成绩数据");
        }
        
        System.out.println("导出课程ID为 " + courseId + " 的成绩数据，共 " + scores.size() + " 条记录");
    }
    
    @Override
    public ScoreStatisticsVO getCourseStatistics(Long courseId) {
        assertCanReadCourse(courseId);
        List<Score> scores = scoreMapper.selectByCourseId(courseId);
        Course course = courseMapper.selectById(courseId);
        
        ScoreStatisticsVO statistics = new ScoreStatisticsVO();
        statistics.setCourseId(courseId);
        
        if (course != null) {
            statistics.setCourseName(course.getCourseName());
        }
        
        // 总人数（选课人数）
        int totalCount = scores != null ? scores.size() : 0;
        statistics.setTotalCount(totalCount);
        
        if (totalCount == 0) {
            return statistics;
        }
        
        // 初始化计数器
        int evaluatedCount = 0;
        int excellentCount = 0;  // >=90
        int goodCount = 0;       // >=80 && <90
        int passCount = 0;       // >=60
        int failCount = 0;       // <60
        
        BigDecimal sumTotalScore = BigDecimal.ZERO;
        BigDecimal sumUsualScore = BigDecimal.ZERO;
        BigDecimal sumFinalScore = BigDecimal.ZERO;
        BigDecimal maxScore = BigDecimal.ZERO;
        BigDecimal minScore = new BigDecimal("100");
        
        for (Score score : scores) {
            // 检查是否已评定成绩
            if (score.getTotalScore() != null && score.getTotalScore().compareTo(BigDecimal.ZERO) > 0) {
                evaluatedCount++;
                BigDecimal totalScore = score.getTotalScore();
                
                // 累加分数
                sumTotalScore = sumTotalScore.add(totalScore);
                if (score.getUsualScore() != null) {
                    sumUsualScore = sumUsualScore.add(score.getUsualScore());
                }
                if (score.getFinalScore() != null) {
                    sumFinalScore = sumFinalScore.add(score.getFinalScore());
                }
                
                // 更新最高分和最低分
                if (totalScore.compareTo(maxScore) > 0) {
                    maxScore = totalScore;
                }
                if (totalScore.compareTo(minScore) < 0) {
                    minScore = totalScore;
                }
                
                // 统计各等级人数
                double scoreValue = totalScore.doubleValue();
                if (scoreValue >= 90) {
                    excellentCount++;
                } else if (scoreValue >= 80) {
                    goodCount++;
                } else if (scoreValue >= 60) {
                    passCount++;
                } else {
                    failCount++;
                }
            }
        }
        
        // 设置统计数据
        statistics.setEvaluatedCount(evaluatedCount);
        statistics.setUnevaluatedCount(totalCount - evaluatedCount);
        statistics.setExcellentCount(excellentCount);
        statistics.setGoodCount(goodCount);
        statistics.setPassCount(passCount);
        statistics.setFailCount(failCount);
        
        // 计算平均分
        if (evaluatedCount > 0) {
            statistics.setAverageScore(sumTotalScore.divide(new BigDecimal(evaluatedCount), 2, RoundingMode.HALF_UP));
            statistics.setMaxScore(maxScore);
            statistics.setMinScore(minScore);
            
            if (sumUsualScore.compareTo(BigDecimal.ZERO) > 0) {
                statistics.setAverageUsualScore(sumUsualScore.divide(new BigDecimal(evaluatedCount), 2, RoundingMode.HALF_UP));
            }
            if (sumFinalScore.compareTo(BigDecimal.ZERO) > 0) {
                statistics.setAverageFinalScore(sumFinalScore.divide(new BigDecimal(evaluatedCount), 2, RoundingMode.HALF_UP));
            }
        }
        
        // 计算百分比
        statistics.calculateRates();
        
        return statistics;
    }

    private SysUser currentUser() {
        Object principal = SecurityUtils.getSubject().getPrincipal();
        if (!(principal instanceof SysUser)) throw new BusinessException(401, "未登录");
        return (SysUser) principal;
    }

    private Student currentStudent() {
        Student student = studentMapper.selectByUserId(currentUser().getUserId());
        if (student == null) throw BusinessException.forbidden("当前账号未关联学生身份");
        return student;
    }

    private Teacher currentTeacher() {
        Teacher teacher = teacherMapper.selectByUserId(currentUser().getUserId());
        if (teacher == null) throw BusinessException.forbidden("当前账号未关联教师身份");
        return teacher;
    }

    private void assertCanReadSelection(CourseSelection selection) {
        if (selection == null) throw BusinessException.notFound("选课记录不存在");
        if (SecurityUtils.getSubject().hasRole("ADMIN")) return;
        if (SecurityUtils.getSubject().hasRole("STUDENT")) {
            if (!currentStudent().getStudentId().equals(selection.getStudentId())) throw BusinessException.forbidden("只能查看自己的成绩");
        } else {
            assertCanReadCourse(selection.getCourseId());
        }
    }

    private void assertCanReadCourse(Long courseId) {
        if (SecurityUtils.getSubject().hasRole("ADMIN")) return;
        if (SecurityUtils.getSubject().hasRole("STUDENT")) throw BusinessException.forbidden("无权查看课程成绩");
        Course course = courseMapper.selectById(courseId);
        if (course == null || !currentTeacher().getTeacherId().equals(course.getTeacherId())) {
            throw BusinessException.forbidden("只能查看本人授课课程的成绩");
        }
    }

    private void assertCanGradeSelection(CourseSelection selection) {
        if (selection == null) throw BusinessException.notFound("选课记录不存在");
        if (SecurityUtils.getSubject().hasRole("ADMIN")) return;
        assertCanReadCourse(selection.getCourseId());
    }
}
