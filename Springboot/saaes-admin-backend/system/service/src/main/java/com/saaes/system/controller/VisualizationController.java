package com.saaes.system.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.saaes.common.core.web.MyException;
import com.saaes.common.core.web.RestResponse;
import com.saaes.system.client.dto.AiDataStatsDTO;
import com.saaes.system.client.dto.DailyScoreAvgDTO;
import com.saaes.system.client.dto.TypeCountDTO;
import com.saaes.system.service.VisualizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "数据可视化")
@RestController
@RequestMapping("/api/visualization")
public class VisualizationController {

    @Resource
    private VisualizationService visualizationService;

    @SaIgnore
    @Operation(description = "历史记录30天平均成绩")
    @GetMapping("/queryHistoricalTrends")
    public RestResponse<List<DailyScoreAvgDTO>> getUserDailyScores() {
        List<DailyScoreAvgDTO> result = visualizationService.getLastMonthDailyAvgScores();
        if (result == null || result.isEmpty()) {throw new MyException("无相关数据");}
        return RestResponse.success(result);
    }

    @SaIgnore
    @Operation(description = "聊天记录总次数和最近30天次数")
    @GetMapping("/queryContentCount")
    public RestResponse<List<TypeCountDTO>> getTypeCountByCreateBy() {
        List<TypeCountDTO> result = visualizationService.getTypeCountByCreateBy();
        if (result == null || result.isEmpty()) {throw new MyException("无相关数据");}
        return RestResponse.success(result);
    }

    @SaIgnore
    @Operation(description = "获取指定用户的卡片和笔记统计数据")
    @GetMapping("/getStatsByCreateBy")
    public RestResponse<AiDataStatsDTO> getStatsByCreateBy() {
        AiDataStatsDTO aiDataStatsDTO = visualizationService.getAiDataStats();
        if (aiDataStatsDTO == null) {throw new MyException("无相关数据");}
        return RestResponse.success("查询成功", aiDataStatsDTO);
    }

    @SaIgnore
    @Operation(description = "根据动作名称查询用户动作平均分")
    @GetMapping("/getMotionNameAnalysis")
    public RestResponse<DailyScoreAvgDTO> getMotionNameAnalysis(@RequestParam String motionName) {
        DailyScoreAvgDTO averageScores = visualizationService.getAverageScores(motionName);
        if (averageScores == null) {throw new MyException("无相关数据");}
        return RestResponse.success(averageScores);
    }


}
