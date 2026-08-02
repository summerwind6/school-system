package com.school.system.service.impl;

import com.school.system.common.PageResult;
import com.school.system.dto.CourseDTO;
import com.school.system.entity.Course;
import com.school.system.mapper.CourseMapper;
import com.school.system.mapper.TeacherMapper;
import com.school.system.entity.Teacher;
import com.school.system.common.BusinessException;
import com.school.system.service.CourseService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import com.school.system.entity.SysUser;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CourseServiceImpl implements CourseService {
    
    @Autowired
    private CourseMapper courseMapper;

    @Autowired
    private TeacherMapper teacherMapper;
    
    @Override
    @Transactional
    public void addCourse(CourseDTO courseDTO) {
        Course existCourse = courseMapper.selectByCourseCode(courseDTO.getCourseCode());
        if (existCourse != null) {
            throw new RuntimeException("课程编码已存在");
        }
        
        Course course = new Course();
        BeanUtils.copyProperties(courseDTO, course);
        
        Subject subject = SecurityUtils.getSubject();
        boolean isAdmin = subject.hasRole("ADMIN");
        
        if (isAdmin) {
            course.setAuditStatus(1);
            course.setStatus(courseDTO.getStatus() != null ? courseDTO.getStatus() : 1);
        } else {
            Teacher teacher = currentTeacher();
            // 教师不能借由请求体指定其他教师；课程归属来自登录身份。
            course.setTeacherId(teacher.getTeacherId());
            course.setAuditStatus(0);
            course.setStatus(1);
        }
        
        courseMapper.insert(course);
    }
    
    @Override
    @Transactional
    public void updateCourse(CourseDTO courseDTO) {
        if (courseDTO.getCourseId() == null) {
            throw BusinessException.badRequest("课程ID不能为空");
        }
        Course original = courseMapper.selectById(courseDTO.getCourseId());
        if (original == null) {
            throw BusinessException.notFound("课程不存在");
        }
        assertCourseOwner(original);
        Course existCourse = courseMapper.selectByCourseCode(courseDTO.getCourseCode());
        if (existCourse != null && !existCourse.getCourseId().equals(courseDTO.getCourseId())) {
            throw new RuntimeException("课程编码已存在");
        }
        
        Course course = new Course();
        BeanUtils.copyProperties(courseDTO, course);
        course.setTeacherId(original.getTeacherId());
        course.setAuditStatus(original.getAuditStatus());
        course.setStatus(courseDTO.getStatus() == null ? original.getStatus() : courseDTO.getStatus());
        
        courseMapper.update(course);
    }
    
    @Override
    @Transactional
    public void deleteCourse(Long courseId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) throw BusinessException.notFound("课程不存在");
        assertCourseOwner(course);
        if (course.getSelectedCount() != null && course.getSelectedCount() > 0) {
            throw BusinessException.conflict("已有学生选课的课程不能删除，请先停用");
        }
        courseMapper.deleteById(courseId);
    }
    
    @Override
    public Course getCourseById(Long courseId) {
        return courseMapper.selectById(courseId);
    }
    
    @Override
    public List<Course> getAllCourses() {
        return courseMapper.selectAll();
    }
    
    @Override
    public PageResult<Course> getCoursePage(int current, int size, String courseName) {
        validatePage(current, size);
        int offset = (current - 1) * size;
        List<Course> records = courseMapper.selectPage(offset, size, courseName);
        int total = courseMapper.countTotal(courseName);
        return PageResult.of((long) total, records, (long) current, (long) size);
    }
    
    @Override
    @Transactional
    public void approveCourse(Long courseId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new RuntimeException("课程不存在");
        }
        course.setAuditStatus(1);
        course.setStatus(1);
        courseMapper.update(course);
    }
    
    @Override
    @Transactional
    public void rejectCourse(Long courseId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new RuntimeException("课程不存在");
        }
        course.setAuditStatus(2);
        courseMapper.update(course);
    }
    
    @Override
    @Transactional
    public void updateCourseStatus(Long courseId, Integer status) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new RuntimeException("课程不存在");
        }
        course.setStatus(status);
        courseMapper.update(course);
    }
    
    @Override
    public List<Course> getPendingCourses() {
        return courseMapper.selectByAuditStatus(0);
    }
    
    @Override
    public List<Course> getApprovedCourses() {
        return courseMapper.selectApproved();
    }
    
    @Override
    public PageResult<Course> getPendingCoursePage(int current, int size, String courseName) {
        validatePage(current, size);
        List<Course> pendingCourses = courseMapper.selectByAuditStatus(0);
        
        if (courseName != null && !courseName.isEmpty()) {
            pendingCourses = pendingCourses.stream()
                .filter(course -> course.getCourseName().contains(courseName))
                .collect(Collectors.toList());
        }
        
        int total = pendingCourses.size();
        int offset = (current - 1) * size;
        if (offset >= total) {
            return PageResult.of((long) total, java.util.Collections.emptyList(), (long) current, (long) size);
        }
        int end = Math.min(offset + size, total);
        List<Course> records = pendingCourses.subList(offset, end);
        
        return PageResult.of((long) total, records, (long) current, (long) size);
    }

    private Teacher currentTeacher() {
        Object principal = SecurityUtils.getSubject().getPrincipal();
        if (!(principal instanceof SysUser)) throw new BusinessException(401, "未登录");
        Teacher teacher = teacherMapper.selectByUserId(((SysUser) principal).getUserId());
        if (teacher == null) throw BusinessException.forbidden("当前账号未关联教师身份");
        return teacher;
    }

    private void assertCourseOwner(Course course) {
        if (!SecurityUtils.getSubject().hasRole("ADMIN")) {
            if (course.getTeacherId() == null || !course.getTeacherId().equals(currentTeacher().getTeacherId())) {
                throw BusinessException.forbidden("只能操作本人创建的课程");
            }
        }
    }

    private void validatePage(int current, int size) {
        if (current < 1 || size < 1 || size > 100) {
            throw BusinessException.badRequest("分页参数不合法：current >= 1，size 在 1 到 100 之间");
        }
    }
}
