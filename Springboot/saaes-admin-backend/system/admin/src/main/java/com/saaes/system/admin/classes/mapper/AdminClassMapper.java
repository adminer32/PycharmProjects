package com.saaes.system.admin.classes.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.saaes.system.admin.classes.vo.AdminClassVO;
import com.saaes.system.client.entity.SysClass;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AdminClassMapper extends BaseMapper<SysClass> {

    /**
     * 班级分页查询
     */
    IPage<AdminClassVO> selectClassPage(IPage<AdminClassVO> page, @Param(Constants.WRAPPER) Wrapper<SysClass> wrapper);

    /**
     * 获取班级详情
     */
    AdminClassVO selectClassDetail(@Param("id") Integer id);

    /**
     * 查询已删除的班级 (回收站)
     */
    IPage<AdminClassVO> selectDeletedPage(IPage<AdminClassVO> page, @Param(Constants.WRAPPER) Wrapper<SysClass> wrapper);

    /**
     * 物理删除已删除的过期班级
     */
    int deleteHardExpired(@Param("threshold") LocalDateTime threshold);

    /**
     * 恢复已删除数据
     */
    int restoreByIds(@Param("ids") List<Integer> ids);
}
