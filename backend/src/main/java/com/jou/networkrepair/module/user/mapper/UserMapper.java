package com.jou.networkrepair.module.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jou.networkrepair.module.user.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<SysUser> {

    @Select("SELECT * FROM `user` WHERE username = #{account} OR employee_no = #{account} LIMIT 1")
    SysUser findByAccount(@Param("account") String account);

    @Select("SELECT COUNT(1) FROM `user` WHERE employee_no = #{employeeNo} AND (#{excludeId} IS NULL OR id <> #{excludeId})")
    Long countByEmployeeNo(@Param("employeeNo") String employeeNo, @Param("excludeId") Long excludeId);
}
