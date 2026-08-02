package com.school.system.controller;

import com.school.system.common.Result;
import com.school.system.entity.Department;
import com.school.system.mapper.DepartmentMapper;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/department")
public class DepartmentController {
    
    @Autowired
    private DepartmentMapper departmentMapper;
    
    @GetMapping("/list")
    @RequiresRoles(logical = org.apache.shiro.authz.annotation.Logical.OR, value = {"TEACHER", "ADMIN"})
    public Result<List<Department>> getDepartmentList() {
        try {
            List<Department> departments = departmentMapper.selectAll();
            return Result.success(departments);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
