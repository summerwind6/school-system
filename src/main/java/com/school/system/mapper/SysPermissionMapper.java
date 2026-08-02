package com.school.system.mapper;

import com.school.system.entity.SysPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysPermissionMapper {
    
    int insert(SysPermission sysPermission);
    
    int update(SysPermission sysPermission);
    
    int deleteById(Long permissionId);
    
    SysPermission selectById(Long permissionId);
    
    List<SysPermission> selectByRoleId(Long roleId);
    
    List<SysPermission> selectAll();
}