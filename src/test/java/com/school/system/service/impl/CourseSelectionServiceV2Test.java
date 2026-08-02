package com.school.system.service.impl;

import com.school.system.common.BusinessException;
import com.school.system.dto.CourseSelectionDTO;
import com.school.system.entity.Course;
import com.school.system.entity.CourseSchedule;
import com.school.system.entity.CourseSelection;
import com.school.system.entity.Student;
import com.school.system.entity.SysUser;
import com.school.system.mapper.CourseMapper;
import com.school.system.mapper.CourseScheduleMapper;
import com.school.system.mapper.CourseSelectionMapper;
import com.school.system.mapper.StudentMapper;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CourseSelectionServiceV2Test {
    private CourseSelectionServiceV2 service;
    private CourseMapper courseMapper;
    private CourseSelectionMapper selectionMapper;
    private CourseScheduleMapper scheduleMapper;

    @BeforeEach
    void setUp() {
        courseMapper = mock(CourseMapper.class);
        selectionMapper = mock(CourseSelectionMapper.class);
        scheduleMapper = mock(CourseScheduleMapper.class);
        StudentMapper studentMapper = mock(StudentMapper.class);
        Student student = new Student();
        student.setStudentId(101L);
        when(studentMapper.selectByUserId(1L)).thenReturn(student);
        service = new CourseSelectionServiceV2();
        ReflectionTestUtils.setField(service, "courseMapper", courseMapper);
        ReflectionTestUtils.setField(service, "courseSelectionMapper", selectionMapper);
        ReflectionTestUtils.setField(service, "courseScheduleMapper", scheduleMapper);
        ReflectionTestUtils.setField(service, "studentMapper", studentMapper);

        SysUser user = new SysUser();
        user.setUserId(1L);
        DefaultSecurityManager manager = new DefaultSecurityManager();
        SecurityUtils.setSecurityManager(manager);
        Subject subject = new Subject.Builder(manager)
                .principals(new SimplePrincipalCollection(user, "test"))
                .buildSubject();
        ThreadContext.bind(subject);
    }

    @AfterEach
    void tearDown() {
        ThreadContext.unbindSubject();
        SecurityUtils.setSecurityManager(null);
    }

    @Test
    void selectCourseUsesLoggedInStudentRatherThanClientStudentId() {
        Course course = new Course();
        course.setCourseId(2L);
        when(courseMapper.selectById(2L)).thenReturn(course);
        when(scheduleMapper.selectByCourseId(2L)).thenReturn(Collections.emptyList());
        when(selectionMapper.selectActiveByStudentId(101L)).thenReturn(Collections.emptyList());
        when(courseMapper.reserveSeat(2L)).thenReturn(1);

        CourseSelectionDTO request = request();
        request.setStudentId(999L);
        service.selectCourse(request);

        ArgumentCaptor<CourseSelection> captor = ArgumentCaptor.forClass(CourseSelection.class);
        verify(selectionMapper).insert(captor.capture());
        assertEquals(101L, captor.getValue().getStudentId());
        assertEquals(1, captor.getValue().getStatus());
    }

    @Test
    void fullCourseDoesNotCreateSelection() {
        when(courseMapper.selectById(2L)).thenReturn(new Course());
        when(scheduleMapper.selectByCourseId(2L)).thenReturn(Collections.emptyList());
        when(selectionMapper.selectActiveByStudentId(101L)).thenReturn(Collections.emptyList());
        when(courseMapper.reserveSeat(2L)).thenReturn(0);

        BusinessException error = assertThrows(BusinessException.class, () -> service.selectCourse(request()));

        assertEquals(409, error.getStatus());
        verify(selectionMapper, never()).insert(any());
    }

    @Test
    void oddAndEvenWeeksDoNotConflictButAllWeekDoes() {
        when(courseMapper.selectById(2L)).thenReturn(new Course());
        CourseSelection selected = new CourseSelection();
        selected.setCourseId(1L);
        selected.setSemester("春季");
        selected.setYear("2026-2027");
        when(selectionMapper.selectActiveByStudentId(101L)).thenReturn(Collections.singletonList(selected));
        when(scheduleMapper.selectByCourseId(1L)).thenReturn(Collections.singletonList(schedule("ODD")));
        when(scheduleMapper.selectByCourseId(2L)).thenReturn(Collections.singletonList(schedule("EVEN")));
        assertEquals(false, service.checkConflict(null, 2L, "春季", "2026-2027"));

        when(scheduleMapper.selectByCourseId(2L)).thenReturn(Collections.singletonList(schedule("ALL")));
        assertEquals(true, service.checkConflict(null, 2L, "春季", "2026-2027"));
    }

    private CourseSelectionDTO request() {
        CourseSelectionDTO dto = new CourseSelectionDTO();
        dto.setCourseId(2L);
        dto.setSemester("春季");
        dto.setYear("2026-2027");
        return dto;
    }

    private CourseSchedule schedule(String weekType) {
        CourseSchedule schedule = new CourseSchedule();
        schedule.setDayOfWeek(1);
        schedule.setStartSection(1);
        schedule.setEndSection(2);
        schedule.setStartWeek(1);
        schedule.setEndWeek(16);
        schedule.setWeekType(weekType);
        return schedule;
    }
}
