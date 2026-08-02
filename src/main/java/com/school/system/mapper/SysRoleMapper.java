package com.school.system.mapper;

import com.school.system.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysRoleMapper {
    
    int insert(SysRole sysRole);
    
    int update(SysRole sysRole);
    
    int deleteById(Long roleId);
    
    SysRole selectById(Long roleId);
    
    SysRole selectByRoleCode(String roleCode);
    
    List<SysRole> selectByUserId(Long userId);
    
    List<SysRole> selectAll();
    
    void insertUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);
    
    void deleteUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);
}