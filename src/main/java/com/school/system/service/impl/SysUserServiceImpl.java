package com.school.system.service.impl;

import com.school.system.dto.LoginDTO;
import com.school.system.dto.RegisterDTO;
import com.school.system.dto.UserDTO;
import com.school.system.entity.*;
import com.school.system.mapper.*;
import com.school.system.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SysUserServiceImpl implements SysUserService {
    
    @Autowired
    private SysUserMapper sysUserMapper;
    
    @Autowired
    private SysRoleMapper sysRoleMapper;
    
    @Autowired
    private SysPermissionMapper sysPermissionMapper;
    
    @Autowired
    private StudentMapper studentMapper;
    
    @Autowired
    private TeacherMapper teacherMapper;
    
    // SHA-256加密方法
    private String sha256Encode(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("密码加密失败", e);
        }
    }
    
    @Override
    public UserDTO login(LoginDTO loginDTO) {
        SysUser user = sysUserMapper.selectByUsername(loginDTO.getUsername());
        if (user == null) {
            throw new RuntimeException("学号/工号不存在");
        }
        
        String encryptedPassword = sha256Encode(loginDTO.getPassword());
        if (!user.getPassword().equals(encryptedPassword)) {
            throw new RuntimeException("密码错误");
        }
        
        if (user.getStatus() == 0) {
            throw new RuntimeException("账号已被禁用");
        }
        
        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(user.getUserId());
        userDTO.setUsername(user.getUsername());
        userDTO.setRealName(user.getRealName());
        userDTO.setGender(user.getGender());
        userDTO.setPhone(user.getPhone());
        userDTO.setEmail(user.getEmail());
        userDTO.setStatus(user.getStatus());
        userDTO.setCreateTime(user.getCreateTime());
        userDTO.setUpdateTime(user.getUpdateTime());
        
        List<SysRole> roles = sysRoleMapper.selectByUserId(user.getUserId());
        List<String> roleCodes = roles.stream().map(SysRole::getRoleCode).collect(Collectors.toList());
        userDTO.setRoles(roleCodes);
        
        List<String> permissions = roles.stream()
            .flatMap(role -> sysPermissionMapper.selectByRoleId(role.getRoleId()).stream())
            .map(SysPermission::getPermissionCode)
            .collect(Collectors.toList());
        userDTO.setPermissions(permissions);
        
        Student student = studentMapper.selectByUserId(user.getUserId());
        if (student != null) {
            userDTO.setStudentId(student.getStudentId());
        }
        
        Teacher teacher = teacherMapper.selectByUserId(user.getUserId());
        if (teacher != null) {
            userDTO.setTeacherId(teacher.getTeacherId());
        }
        
        return userDTO;
    }
    
    @Override
    @Transactional
    public void register(RegisterDTO registerDTO) {
        // 生成用户名：学生使用学号，教师使用教师工号
        String username = null;
        if (registerDTO.getRoleType() == 1 && registerDTO.getStudentNo() != null) {
            username = registerDTO.getStudentNo();
        } else if (registerDTO.getRoleType() == 2 && registerDTO.getTeacherNo() != null) {
            username = registerDTO.getTeacherNo();
        } else {
            throw new RuntimeException("请提供学号或教师工号");
        }
        
        // 检查用户名是否已存在
        SysUser existUser = sysUserMapper.selectByUsername(username);
        if (existUser != null) {
            throw new RuntimeException("该学号/教师工号已存在");
        }
        
        // 创建用户
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(sha256Encode(registerDTO.getPassword()));
        user.setPhone(registerDTO.getPhone());
        user.setStatus(1);
        
        // 设置真实姓名：学生使用"学生"+学号，教师使用提供的姓名
        if (registerDTO.getRoleType() == 1) {
            // 学生：使用"学生"+学号作为真实姓名
            user.setRealName("学生" + registerDTO.getStudentNo());
        } else if (registerDTO.getRoleType() == 2 && registerDTO.getRealName() != null) {
            // 教师：使用提供的真实姓名
            user.setRealName(registerDTO.getRealName());
        }
        
        sysUserMapper.insert(user);
        
        // 分配角色
        SysRole role = null;
        if (registerDTO.getRoleType() == 1) {
            role = sysRoleMapper.selectByRoleCode("STUDENT");
        } else if (registerDTO.getRoleType() == 2) {
            role = sysRoleMapper.selectByRoleCode("TEACHER");
        } else if (registerDTO.getRoleType() == 3) {
            role = sysRoleMapper.selectByRoleCode("ADMIN");
        }
        
        if (role != null) {
            sysRoleMapper.insertUserRole(user.getUserId(), role.getRoleId());
        }
        
        // 创建学生或教师记录
        if (registerDTO.getRoleType() == 1) {
            Student student = new Student();
            student.setUserId(user.getUserId());
            student.setStudentNo(registerDTO.getStudentNo());
            // 设置默认院系ID为1（计算机科学与技术学院）
            student.setDeptId(1L);
            studentMapper.insert(student);
        } else if (registerDTO.getRoleType() == 2) {
            Teacher teacher = new Teacher();
            teacher.setUserId(user.getUserId());
            // 设置默认院系ID为1（计算机科学与技术学院）
            teacher.setDeptId(1L);
            teacherMapper.insert(teacher);
        }
    }
    
    @Override
    public SysUser getUserById(Long userId) {
        return sysUserMapper.selectById(userId);
    }
    
    @Override
    public SysUser getUserByUsername(String username) {
        return sysUserMapper.selectByUsername(username);
    }
    
    @Override
    public List<SysUser> getAllUsers() {
        return sysUserMapper.selectAll();
    }
    
    @Override
    public void updateUser(SysUser sysUser) {
        sysUserMapper.update(sysUser);
    }
    
    @Override
    public void deleteUser(Long userId) {
        sysUserMapper.deleteById(userId);
    }
    
    @Override
    public void updateStatus(Long userId, Integer status) {
        sysUserMapper.updateStatus(userId, status);
    }
}