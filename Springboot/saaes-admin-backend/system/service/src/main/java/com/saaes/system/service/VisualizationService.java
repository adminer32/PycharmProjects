package com.saaes.system.service;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.saaes.common.core.web.RestResponse;
import com.saaes.system.client.dto.AiDataStatsDTO;
import com.saaes.system.client.dto.DailyScoreAvgDTO;
import com.saaes.system.client.dto.TypeCountDTO;
import com.saaes.system.mapper.VisualizationMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class VisualizationService {

    @Resource
    private VisualizationMapper visualizationMapper;

    public List<DailyScoreAvgDTO> getLastMonthDailyAvgScores() {
        Long loginId = StpUtil.getLoginIdAsLong();
        return visualizationMapper.getDailyAverageScores(loginId);
    }

    public List<TypeCountDTO> getTypeCountByCreateBy() {
        Long loginId = StpUtil.getLoginIdAsLong();
        return visualizationMapper.getTypeCountByCreateBy(loginId);
    }

    public DailyScoreAvgDTO getAverageScores(String motionName) {
        Long loginId = StpUtil.getLoginIdAsLong();
        return visualizationMapper.getAverageScores(loginId, motionName);
    }

    public AiDataStatsDTO getAiDataStats() {
        Long loginId = StpUtil.getLoginIdAsLong();
        Map<String, Object> cardMap = visualizationMapper.getCardStatsByCreateBy(loginId);
        Map<String, Object> noteMap = visualizationMapper.getNoteStatsByCreateBy(loginId);

        AiDataStatsDTO dto = new AiDataStatsDTO();

        AiDataStatsDTO.CardStats cardStats = new AiDataStatsDTO.CardStats();
        cardStats.setTotalCount(((Number) cardMap.get("totalCount")).intValue());
        cardStats.setTodayCount(((Number) cardMap.get("todayCount")).intValue());
        cardStats.setLast30DaysCount(((Number) cardMap.get("last30DaysCount")).intValue());
        dto.setCardStats(cardStats);

        AiDataStatsDTO.NoteStats noteStats = new AiDataStatsDTO.NoteStats();
        noteStats.setTotalCount(((Number) noteMap.get("totalCount")).intValue());
        noteStats.setTodayCount(((Number) noteMap.get("todayCount")).intValue());
        noteStats.setLast30DaysCount(((Number) noteMap.get("last30DaysCount")).intValue());
        dto.setNoteStats(noteStats);

        return dto;
    }
}
