package com.system.service.learning.mapper;

import com.system.service.learning.entity.ActionVideo;
import com.system.service.learning.vo.ActionVideoVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface ActionVideoMapper {

    List<ActionVideoVO.VideoInfo> getVideoList(@Param("classId") String classId, @Param("category") String category);

    int insertActionVideo(ActionVideo video);

    int insertRecommendation(Map<String, Object> params);
}
