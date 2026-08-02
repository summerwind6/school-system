package com.school.system.controller;

import com.school.system.common.Result;
import com.school.system.dto.LoginDTO;
import com.school.system.dto.RegisterDTO;
import com.school.system.dto.UserDTO;
import com.school.system.entity.*;
import com.school.system.mapper.*;
import com.school.system.service.SysUserService;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth")
public class AuthController {
    
    @Autowired
    private SysUserService sysUserService;
    
    @Autowired
    private SysRoleMapper sysRoleMapper;
    
    @Autowired
    private SysPermissionMapper sysPermissionMapper;
    
    @Autowired
    private StudentMapper studentMapper;
    
    @Autowired
    private TeacherMapper teacherMapper;
    
    @PostMapping("/login")
    public Result<UserDTO> login(@Validated @RequestBody LoginDTO loginDTO) {
        try {
            Subject subject = SecurityUtils.getSubject();
            UsernamePasswordToken token = new UsernamePasswordToken(loginDTO.getUsername(), loginDTO.getPassword());
            subject.login(token);
            
            UserDTO user = sysUserService.login(loginDTO);
            return Result.success("登录成功", user);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    @PostMapping("/register")
    public Result<String> register(@Validated @RequestBody RegisterDTO registerDTO) {
        try {
            sysUserService.register(registerDTO);
            return Result.success("注册成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    @PostMapping("/logout")
    public Result<String> logout() {
        Subject subject = SecurityUtils.getSubject();
        subject.logout();
        return Result.success("退出成功");
    }
    
    @GetMapping("/unauthorized")
    public Result<String> unauthorized() {
        return Result.error(401, "未登录");
    }
    
    @GetMapping("/forbidden")
    public Result<String> forbidden() {
        return Result.error(403, "无权限访问");
    }
    
    @GetMapping("/current")
    public Result<UserDTO> getCurrentUser() {
        Subject subject = SecurityUtils.getSubject();
        SysUser sysUser = (SysUser) subject.getPrincipal();
        if (sysUser == null) {
            return Result.error(401, "未登录");
        }
        
        try {
            // 直接查询用户信息，不验证密码
            UserDTO userDTO = new UserDTO();
            userDTO.setUserId(sysUser.getUserId());
            userDTO.setUsername(sysUser.getUsername());
            userDTO.setRealName(sysUser.getRealName());
            userDTO.setGender(sysUser.getGender());
            userDTO.setPhone(sysUser.getPhone());
            userDTO.setEmail(sysUser.getEmail());
            userDTO.setStatus(sysUser.getStatus());
            userDTO.setCreateTime(sysUser.getCreateTime());
            userDTO.setUpdateTime(sysUser.getUpdateTime());
            
            // 查询角色
            List<SysRole> roles = sysRoleMapper.selectByUserId(sysUser.getUserId());
            List<String> roleCodes = roles.stream().map(SysRole::getRoleCode).collect(Collectors.toList());
            userDTO.setRoles(roleCodes);
            
            // 查询权限
            List<String> permissions = roles.stream()
                .flatMap(role -> sysPermissionMapper.selectByRoleId(role.getRoleId()).stream())
                .map(SysPermission::getPermissionCode)
                .collect(Collectors.toList());
            userDTO.setPermissions(permissions);
            
            // 查询学生/教师信息
            Student student = studentMapper.selectByUserId(sysUser.getUserId());
            if (student != null) {
                userDTO.setStudentId(student.getStudentId());
            }
            
            Teacher teacher = teacherMapper.selectByUserId(sysUser.getUserId());
            if (teacher != null) {
                userDTO.setTeacherId(teacher.getTeacherId());
            }
            
            return Result.success(userDTO);
        } catch (Exception e) {
            return Result.error("获取用户信息失败");
        }
    }
}