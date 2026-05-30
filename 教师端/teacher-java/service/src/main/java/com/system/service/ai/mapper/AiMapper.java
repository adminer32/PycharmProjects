package com.system.service.ai.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface AiMapper {

    Map<String, Object> getStudentBasicInfo(@Param("studentId") Long studentId);

    List<Map<String, Object>> getStudentWeakness(@Param("studentId") Long studentId);

    List<Map<String, Object>> getStudentRecentActivity(@Param("studentId") Long studentId);
}
