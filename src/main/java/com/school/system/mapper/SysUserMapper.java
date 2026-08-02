package com.school.system.mapper;

import com.school.system.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SysUserMapper {
    
    int insert(SysUser sysUser);
    
    int update(SysUser sysUser);
    
    int deleteById(Long userId);
    
    SysUser selectById(Long userId);
    
    SysUser selectByUsername(String username);
    
    int updateStatus(@Param("userId") Long userId, @Param("status") Integer status);
    
    java.util.List<SysUser> selectAll();
}