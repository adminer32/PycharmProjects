package com.saaes.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.saaes.system.client.entity.AIAnalysisInfo;
import org.apache.ibatis.annotations.*;

@Mapper
public interface AIAnalysisInfoMapper extends BaseMapper<AIAnalysisInfo> {

    int insert(AIAnalysisInfo aiAnalysisInfo);
}
