package com.school.system.service.impl;

import com.school.system.common.PageResult;
import com.school.system.dto.CourseSelectionDTO;
import com.school.system.dto.CourseSelectionVO;
import com.school.system.entity.Course;
import com.school.system.entity.CourseSelection;
import com.school.system.mapper.CourseMapper;
import com.school.system.mapper.CourseSelectionMapper;
import com.school.system.mapper.TeacherMapper;
import com.school.system.service.CourseSelectionService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 保留旧实现仅用于历史比对；实际 Bean 由 CourseSelectionServiceV2 提供。
 */
@Deprecated
class LegacyCourseSelectionServiceImpl implements CourseSelectionService {
    
    @Autowired
    private CourseSelectionMapper courseSelectionMapper;
    
    @Autowired
    private CourseMapper courseMapper;
    
    @Autowired
    private TeacherMapper teacherMapper;
    
    @Override
    @Transactional
    public void selectCourse(CourseSelectionDTO courseSelectionDTO) {
        CourseSelection existSelection = courseSelectionMapper.selectByStudentAndCourse(
            courseSelectionDTO.getStudentId(), 
            courseSelectionDTO.getCourseId(),
            courseSelectionDTO.getSemester(),
            courseSelectionDTO.getYear()
        );
        
        if (existSelection != null) {
            throw new RuntimeException("已选择该课程");
        }
        
        if (checkConflict(courseSelectionDTO.getStudentId(), courseSelectionDTO.getCourseId(), 
            courseSelectionDTO.getSemester(), courseSelectionDTO.getYear())) {
            throw new RuntimeException("选课时间冲突");
        }
        
        Course course = courseMapper.selectById(courseSelectionDTO.getCourseId());
        if (course == null || course.getStatus() != 1) {
            throw new RuntimeException("课程不存在或未启用");
        }
        
        if (course.getAuditStatus() == null || course.getAuditStatus() != 1) {
            throw new RuntimeException("该课程尚未通过审核，无法选课");
        }
        
        CourseSelection selection = new CourseSelection();
        BeanUtils.copyProperties(courseSelectionDTO, selection);
        selection.setStatus(1);
        
        courseSelectionMapper.insert(selection);
    }
    
    @Override
    @Transactional
    public void dropCourse(Long selectionId) {
        CourseSelection selection = courseSelectionMapper.selectById(selectionId);
        if (selection == null) {
            throw new RuntimeException("选课记录不存在");
        }
        
        courseSelectionMapper.deleteById(selectionId);
    }
    
    @Override
    public CourseSelection getSelectionById(Long selectionId) {
        return courseSelectionMapper.selectById(selectionId);
    }
    
    @Override
    public List<CourseSelection> getSelectionsByStudentId(Long studentId) {
        return courseSelectionMapper.selectByStudentId(studentId);
    }
    
    @Override
    public List<CourseSelectionVO> getSelectionVOsByStudentId(Long studentId) {
        List<CourseSelection> selections = courseSelectionMapper.selectByStudentId(studentId);
        List<CourseSelectionVO> voList = new ArrayList<>();
        
        for (CourseSelection selection : selections) {
            CourseSelectionVO vo = new CourseSelectionVO();
            
            // 复制选课基本信息
            vo.setSelectionId(selection.getSelectionId());
            vo.setStudentId(selection.getStudentId());
            vo.setCourseId(selection.getCourseId());
            vo.setSemester(selection.getSemester());
            vo.setYear(selection.getYear());
            vo.setStatus(selection.getStatus());
            vo.setCreateTime(selection.getCreateTime());
            
            // 查询课程信息并填充
            Course course = courseMapper.selectById(selection.getCourseId());
            if (course != null) {
                vo.setCourseName(course.getCourseName());
                vo.setCourseCode(course.getCourseCode());
                vo.setCredits(course.getCredits());
                vo.setHours(course.getHours());
                vo.setDeptId(course.getDeptId());
                vo.setTeacherId(course.getTeacherId());
                vo.setDescription(course.getDescription());
                vo.setCourseStatus(course.getStatus());
                
                // 查询教师姓名（可选）
                if (course.getTeacherId() != null) {
                    try {
                        com.school.system.entity.Teacher teacher = teacherMapper.selectById(course.getTeacherId());
                        if (teacher != null) {
                            // 需要通过用户表获取教师姓名，这里简化处理
                            vo.setTeacherName(teacher.getTitle() + "教师");
                        }
                    } catch (Exception e) {
                        // 忽略查询异常
                    }
                }
            }
            
            voList.add(vo);
        }
        
        return voList;
    }
    
    @Override
    public List<CourseSelection> getSelectionsByCourseId(Long courseId) {
        return courseSelectionMapper.selectByCourseId(courseId);
    }
    
    @Override
    public PageResult<CourseSelection> getSelectionPage(int current, int size, Long studentId, Long courseId) {
        int offset = (current - 1) * size;
        List<CourseSelection> records = courseSelectionMapper.selectPage(offset, size, studentId, courseId);
        int total = courseSelectionMapper.countTotal(studentId, courseId);
        return PageResult.of((long) total, records, (long) current, (long) size);
    }
    
    @Override
    public boolean checkConflict(Long studentId, Long courseId, String semester, String year) {
        List<CourseSelection> existingSelections = courseSelectionMapper.selectByStudentId(studentId);
        Course newCourse = courseMapper.selectById(courseId);
        
        if (newCourse == null) {
            return true;
        }
        
        for (CourseSelection selection : existingSelections) {
            if (selection.getSemester().equals(semester) && selection.getYear().equals(year)) {
                Course existingCourse = courseMapper.selectById(selection.getCourseId());
                
                if (existingCourse != null) {
                    if (hasTimeConflict(existingCourse, newCourse)) {
                        throw new RuntimeException("选课时间冲突：与《" + existingCourse.getCourseName() + "》上课时间冲突");
                    }
                }
            }
        }
        
        int currentCount = courseSelectionMapper.countByCourseIdAndStatus(courseId, 1);
        if (currentCount >= 50) {
            throw new RuntimeException("选课失败：该课程已满员（当前" + currentCount + "人）");
        }
        
        return false;
    }
    
    private boolean hasTimeConflict(Course course1, Course course2) {
        if (course1.getHours() == null || course2.getHours() == null) {
            return false;
        }
        
        if (course1.getHours().equals(course2.getHours())) {
            return true;
        }
        
        if ((course1.getHours() >= 32 && course1.getHours() <= 48) && 
            (course2.getHours() >= 32 && course2.getHours() <= 48)) {
            return true;
        }
        
        if ((course1.getHours() >= 48 && course1.getHours() <= 64) && 
            (course2.getHours() >= 48 && course2.getHours() <= 64)) {
            return true;
        }
        
        return false;
    }
}
