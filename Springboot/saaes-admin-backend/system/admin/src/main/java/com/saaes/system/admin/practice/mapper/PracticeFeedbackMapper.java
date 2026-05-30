package com.saaes.system.admin.practice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.saaes.system.client.entity.PracticeFeedback;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface PracticeFeedbackMapper extends BaseMapper<PracticeFeedback> {
    /**
     * 物理删除已过期练习评价
     */
    int deleteHardExpired(@Param("threshold") LocalDateTime threshold);
}
