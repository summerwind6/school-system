package com.school.system.service.impl;

import com.school.system.common.BusinessException;
import com.school.system.common.PageResult;
import com.school.system.dto.CourseSelectionDTO;
import com.school.system.dto.CourseSelectionVO;
import com.school.system.entity.Course;
import com.school.system.entity.CourseSchedule;
import com.school.system.entity.CourseSelection;
import com.school.system.entity.Student;
import com.school.system.entity.SysUser;
import com.school.system.mapper.CourseMapper;
import com.school.system.mapper.CourseScheduleMapper;
import com.school.system.mapper.CourseSelectionMapper;
import com.school.system.mapper.StudentMapper;
import com.school.system.service.CourseSelectionService;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CourseSelectionServiceV2 implements CourseSelectionService {
    @Autowired private CourseSelectionMapper courseSelectionMapper;
    @Autowired private CourseMapper courseMapper;
    @Autowired private StudentMapper studentMapper;
    @Autowired private CourseScheduleMapper courseScheduleMapper;

    @Override
    @Transactional
    public void selectCourse(CourseSelectionDTO request) {
        Student student = currentStudent();
        CourseSelection existing = courseSelectionMapper.selectByStudentAndCourse(
                student.getStudentId(), request.getCourseId(), request.getSemester(), request.getYear());
        if (existing != null && existing.getStatus() == 1) {
            throw BusinessException.conflict("已选择该课程");
        }
        assertNoScheduleConflict(student.getStudentId(), request.getCourseId(), request.getSemester(), request.getYear());
        // 原子占座：不使用“先 count 再 insert”的竞争窗口。
        if (courseMapper.reserveSeat(request.getCourseId()) != 1) {
            throw BusinessException.conflict("课程不存在、未开放、未通过审核或名额已满");
        }
        try {
            if (existing != null) {
                if (courseSelectionMapper.reactivate(existing.getSelectionId()) != 1) {
                    throw BusinessException.conflict("选课状态已变化，请刷新后重试");
                }
                return;
            }
            CourseSelection selection = new CourseSelection();
            selection.setStudentId(student.getStudentId());
            selection.setCourseId(request.getCourseId());
            selection.setSemester(request.getSemester());
            selection.setYear(request.getYear());
            selection.setStatus(1);
            courseSelectionMapper.insert(selection);
        } catch (DuplicateKeyException e) {
            // RuntimeException 会让事务回滚已占用的名额。
            throw BusinessException.conflict("已选择该课程");
        }
    }

    @Override
    @Transactional
    public void dropCourse(Long selectionId) {
        CourseSelection selection = courseSelectionMapper.selectById(selectionId);
        if (selection == null) throw BusinessException.notFound("选课记录不存在");
        if (!selection.getStudentId().equals(currentStudent().getStudentId())) {
            throw BusinessException.forbidden("不能退选其他学生的课程");
        }
        if (courseSelectionMapper.cancelIfActive(selectionId) != 1) {
            throw BusinessException.conflict("课程已退选，请勿重复提交");
        }
        if (courseMapper.releaseSeat(selection.getCourseId()) != 1) {
            throw new IllegalStateException("课程选课人数不一致，退课已回滚");
        }
    }

    @Override
    public CourseSelection getSelectionById(Long selectionId) {
        CourseSelection selection = courseSelectionMapper.selectById(selectionId);
        if (selection == null) throw BusinessException.notFound("选课记录不存在");
        assertStudentOwner(selection.getStudentId());
        return selection;
    }

    @Override
    public List<CourseSelection> getSelectionsByStudentId(Long studentId) {
        assertStudentOwner(studentId);
        return courseSelectionMapper.selectByStudentId(studentId);
    }

    @Override
    public List<CourseSelectionVO> getSelectionVOsByStudentId(Long studentId) {
        assertStudentOwner(studentId);
        return courseSelectionMapper.selectVOByStudentId(studentId);
    }

    @Override
    public List<CourseSelection> getSelectionsByCourseId(Long courseId) {
        return courseSelectionMapper.selectByCourseId(courseId);
    }

    @Override
    public PageResult<CourseSelection> getSelectionPage(int current, int size, Long studentId, Long courseId) {
        validatePage(current, size);
        if (SecurityUtils.getSubject().hasRole("STUDENT")) studentId = currentStudent().getStudentId();
        int offset = (current - 1) * size;
        List<CourseSelection> records = courseSelectionMapper.selectPage(offset, size, studentId, courseId);
        return PageResult.of((long) courseSelectionMapper.countTotal(studentId, courseId), records, (long) current, (long) size);
    }

    @Override
    public boolean checkConflict(Long ignoredStudentId, Long courseId, String semester, String year) {
        return hasScheduleConflict(currentStudent().getStudentId(), courseId, semester, year);
    }

    private Student currentStudent() {
        Object principal = SecurityUtils.getSubject().getPrincipal();
        if (!(principal instanceof SysUser)) throw new BusinessException(401, "未登录");
        Student student = studentMapper.selectByUserId(((SysUser) principal).getUserId());
        if (student == null) throw BusinessException.forbidden("当前账号未关联学生身份");
        return student;
    }

    private void assertStudentOwner(Long studentId) {
        Subject subject = SecurityUtils.getSubject();
        if (subject.hasRole("STUDENT") && !currentStudent().getStudentId().equals(studentId)) {
            throw BusinessException.forbidden("只能查看自己的选课记录");
        }
    }

    private void assertNoScheduleConflict(Long studentId, Long courseId, String semester, String year) {
        if (hasScheduleConflict(studentId, courseId, semester, year)) throw BusinessException.conflict("选课时间冲突");
    }

    private boolean hasScheduleConflict(Long studentId, Long courseId, String semester, String year) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) throw BusinessException.notFound("课程不存在");
        List<CourseSchedule> candidateSchedules = courseScheduleMapper.selectByCourseId(courseId);
        for (CourseSelection selected : courseSelectionMapper.selectActiveByStudentId(studentId)) {
            if (!semester.equals(selected.getSemester()) || !year.equals(selected.getYear())) continue;
            for (CourseSchedule existingSchedule : courseScheduleMapper.selectByCourseId(selected.getCourseId())) {
                for (CourseSchedule candidate : candidateSchedules) {
                    if (schedulesOverlap(existingSchedule, candidate)) return true;
                }
            }
        }
        return false;
    }

    private boolean schedulesOverlap(CourseSchedule left, CourseSchedule right) {
        return left.getDayOfWeek().equals(right.getDayOfWeek())
                && overlap(left.getStartSection(), left.getEndSection(), right.getStartSection(), right.getEndSection())
                && overlap(left.getStartWeek(), left.getEndWeek(), right.getStartWeek(), right.getEndWeek())
                && ("ALL".equals(left.getWeekType()) || "ALL".equals(right.getWeekType()) || left.getWeekType().equals(right.getWeekType()));
    }

    private boolean overlap(Integer start1, Integer end1, Integer start2, Integer end2) { return start1 <= end2 && start2 <= end1; }

    private void validatePage(int current, int size) {
        if (current < 1 || size < 1 || size > 100) throw BusinessException.badRequest("分页参数不合法：current >= 1，size 在 1 到 100 之间");
    }
}
