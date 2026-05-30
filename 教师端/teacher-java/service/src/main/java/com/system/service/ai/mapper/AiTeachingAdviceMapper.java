package com.system.service.ai.mapper;

import com.system.service.ai.vo.AiTeachingAdvice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface AiTeachingAdviceMapper {

    List<AiTeachingAdvice> selectByTarget(@Param("targetType") String targetType,
                                           @Param("targetId") Long targetId);

    int deleteByTarget(@Param("targetType") String targetType,
                       @Param("targetId") Long targetId);

    int batchInsert(@Param("list") List<AiTeachingAdvice> list);
}
