package com.school.system.service;

import com.school.system.dto.LoginDTO;
import com.school.system.dto.RegisterDTO;
import com.school.system.dto.UserDTO;
import com.school.system.entity.SysUser;

import java.util.List;

public interface SysUserService {
    
    UserDTO login(LoginDTO loginDTO);
    
    void register(RegisterDTO registerDTO);
    
    SysUser getUserById(Long userId);
    
    SysUser getUserByUsername(String username);
    
    List<SysUser> getAllUsers();
    
    void updateUser(SysUser sysUser);
    
    void deleteUser(Long userId);
    
    void updateStatus(Long userId, Integer status);
}