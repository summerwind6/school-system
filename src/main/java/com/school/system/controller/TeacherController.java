package com.school.system.controller;

import com.school.system.common.Result;
import com.school.system.entity.Department;
import com.school.system.entity.Teacher;
import com.school.system.mapper.DepartmentMapper;
import com.school.system.mapper.TeacherMapper;
import com.school.system.mapper.SysUserMapper;
import com.school.system.entity.SysUser;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/teacher")
public class TeacherController {
    
    @Autowired
    private TeacherMapper teacherMapper;
    
    @Autowired
    private SysUserMapper sysUserMapper;
    
    @Autowired
    private DepartmentMapper departmentMapper;
    
    @GetMapping("/list")
    @RequiresRoles(logical = org.apache.shiro.authz.annotation.Logical.OR, value = {"TEACHER", "ADMIN"})
    public Result<List<Map<String, Object>>> getTeacherList() {
        try {
            List<Teacher> teachers = teacherMapper.selectAll();
            List<Map<String, Object>> result = new ArrayList<>();
            for (Teacher teacher : teachers) {
                Map<String, Object> item = new HashMap<>();
                item.put("teacherId", teacher.getTeacherId());
                item.put("userId", teacher.getUserId());
                item.put("deptId", teacher.getDeptId());
                item.put("title", teacher.getTitle());
                item.put("specialty", teacher.getSpecialty());
                
                SysUser user = sysUserMapper.selectById(teacher.getUserId());
                if (user != null) {
                    item.put("realName", user.getRealName());
                }
                
                result.add(item);
            }
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
