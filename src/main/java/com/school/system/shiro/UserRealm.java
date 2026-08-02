package com.school.system.shiro;

import com.school.system.entity.SysPermission;
import com.school.system.entity.SysRole;
import com.school.system.entity.SysUser;
import com.school.system.mapper.SysPermissionMapper;
import com.school.system.mapper.SysRoleMapper;
import com.school.system.mapper.SysUserMapper;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.util.ByteSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class UserRealm extends AuthorizingRealm {
    
    @Autowired
    private SysUserMapper sysUserMapper;
    
    @Autowired
    private SysRoleMapper sysRoleMapper;
    
    @Autowired
    private SysPermissionMapper sysPermissionMapper;
    
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        SysUser user = (SysUser) principals.getPrimaryPrincipal();
        
        SimpleAuthorizationInfo authorizationInfo = new SimpleAuthorizationInfo();
        
        List<SysRole> roles = sysRoleMapper.selectByUserId(user.getUserId());
        Set<String> roleCodes = roles.stream().map(SysRole::getRoleCode).collect(Collectors.toSet());
        authorizationInfo.setRoles(roleCodes);
        
        Set<String> permissions = roles.stream()
            .flatMap(role -> sysPermissionMapper.selectByRoleId(role.getRoleId()).stream())
            .map(SysPermission::getPermissionCode)
            .collect(Collectors.toSet());
        authorizationInfo.setStringPermissions(permissions);
        
        return authorizationInfo;
    }
    
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        UsernamePasswordToken userToken = (UsernamePasswordToken) token;
        String username = userToken.getUsername();
        
        SysUser user = sysUserMapper.selectByUsername(username);
        if (user == null) {
            throw new UnknownAccountException("用户不存在");
        }
        
        if (user.getStatus() == 0) {
            throw new LockedAccountException("账号已被禁用");
        }
        
        // 返回认证信息，使用MD5密码比较
        return new SimpleAuthenticationInfo(
            user, 
            user.getPassword(), 
            getName()
        );
    }
}