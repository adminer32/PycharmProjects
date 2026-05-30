package com.system.service.analysis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.system.service.analysis.entity.MotionAnalysisData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MotionAnalysisMapper extends BaseMapper<MotionAnalysisData> {

    @Select("SELECT * FROM motion_analysis_data WHERE student_id = #{studentId} ORDER BY created_at DESC LIMIT 1")
    MotionAnalysisData findLatestByStudentId(Integer studentId);
}
