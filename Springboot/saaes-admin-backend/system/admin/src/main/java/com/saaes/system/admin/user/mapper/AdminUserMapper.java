package com.saaes.system.admin.user.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.saaes.system.admin.user.vo.AdminUserVO;
import com.saaes.system.client.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AdminUserMapper extends BaseMapper<SysUser> {

    /**
     * 分页查询用户
     */
    IPage<AdminUserVO> selectUserPage(IPage<AdminUserVO> page, @Param(Constants.WRAPPER) Wrapper<SysUser> wrapper);

    /**
     * 查询已删除的用户 (回收站)
     */
    IPage<AdminUserVO> selectRecyclePage(IPage<AdminUserVO> page, @Param(Constants.WRAPPER) Wrapper<SysUser> wrapper);

    /**
     * 恢复已删除用户
     */
    int restoreByIds(@Param("ids") List<Integer> ids);

    /**
     * 物理删除已删除的过期用户
     */
    int deleteHardExpired(@Param("threshold") LocalDateTime threshold);
}
