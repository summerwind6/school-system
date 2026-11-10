package com.school.system.mapper;

import com.school.system.dto.DepartmentOption;
import com.school.system.entity.Department;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DepartmentMapper {
    List<DepartmentOption> selectOptions();
    
    int insert(Department department);
    
    int update(Department department);
    
    int deleteById(Long deptId);
    
    Department selectById(Long deptId);
    
    Department selectByDeptCode(String deptCode);
    
    List<Department> selectAll();
}
