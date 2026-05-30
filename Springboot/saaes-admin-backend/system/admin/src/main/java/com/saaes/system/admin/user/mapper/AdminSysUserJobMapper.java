package com.saaes.system.admin.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.saaes.system.client.entity.SysUserJob;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface AdminSysUserJobMapper extends BaseMapper<SysUserJob> {

    /**
     * 物理删除已过期用户角色关联
     */
    int deleteHardExpired(@Param("threshold") LocalDateTime threshold);
}
