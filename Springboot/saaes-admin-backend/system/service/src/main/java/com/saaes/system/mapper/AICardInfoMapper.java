package com.saaes.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.saaes.system.client.entity.AICardInfo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AICardInfoMapper extends BaseMapper<AICardInfo> {

    // 更新 AICardInfo
    int update(AICardInfo aiCardInfo);
}
