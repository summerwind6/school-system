package com.school.system.service.impl;

import com.school.system.dto.RegisterDTO;
import com.school.system.mapper.StudentMapper;
import com.school.system.mapper.SysPermissionMapper;
import com.school.system.mapper.SysRoleMapper;
import com.school.system.mapper.SysUserMapper;
import com.school.system.mapper.TeacherMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class SysUserServiceImplTest {

    @Test
    void publicRegistrationRejectsAdministratorRole() {
        SysUserServiceImpl service = new SysUserServiceImpl();
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        SysPermissionMapper permissionMapper = mock(SysPermissionMapper.class);
        StudentMapper studentMapper = mock(StudentMapper.class);
        TeacherMapper teacherMapper = mock(TeacherMapper.class);
        ReflectionTestUtils.setField(service, "sysUserMapper", userMapper);
        ReflectionTestUtils.setField(service, "sysRoleMapper", roleMapper);
        ReflectionTestUtils.setField(service, "sysPermissionMapper", permissionMapper);
        ReflectionTestUtils.setField(service, "studentMapper", studentMapper);
        ReflectionTestUtils.setField(service, "teacherMapper", teacherMapper);

        RegisterDTO request = new RegisterDTO();
        request.setRoleType(3);

        assertThrows(IllegalArgumentException.class, () -> service.register(request));
        verifyNoInteractions(userMapper, roleMapper, permissionMapper, studentMapper, teacherMapper);
    }
}
