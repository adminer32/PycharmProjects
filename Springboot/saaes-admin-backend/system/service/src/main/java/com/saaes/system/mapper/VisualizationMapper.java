package com.saaes.system.mapper;

import com.saaes.system.client.dto.AiDataStatsDTO;
import com.saaes.system.client.dto.DailyScoreAvgDTO;
import com.saaes.system.client.dto.TypeCountDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface VisualizationMapper {

    List<DailyScoreAvgDTO> getDailyAverageScores(Long loginId);

    List<TypeCountDTO> getTypeCountByCreateBy(Long loginId);

    // 查询每个 motion_name 下的评分平均值
    @Select("SELECT " +
            "ROUND(AVG(overall_score), 2) AS avg_overall_score, " +
            "ROUND(AVG(stability_score), 2) AS avg_stability_score, " +
            "ROUND(AVG(fluency_score), 2) AS avg_fluency_score, " +
            "ROUND(AVG(proficiency_score), 2) AS avg_proficiency_score " +
            "FROM ai_analysis_result " +
            "WHERE create_by = #{createBy} " +
            "AND motion_name = #{motionName} " +
            "AND task_status = '已完成'")
    DailyScoreAvgDTO getAverageScores(Long createBy, @Param("motionName") String motionName);

    Map<String, Object> getCardStatsByCreateBy(Long createBy);

    Map<String, Object> getNoteStatsByCreateBy(Long createBy);
}
