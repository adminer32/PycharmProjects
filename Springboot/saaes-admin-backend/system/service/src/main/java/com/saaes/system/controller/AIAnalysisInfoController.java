package com.saaes.system.controller;

import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.saaes.common.core.utils.CommonUtil;
import com.saaes.common.core.web.MyException;
import com.saaes.common.core.web.PageQuery;
import com.saaes.common.core.web.RestResponse;
import com.saaes.system.client.dto.ScheduleEventDTO;
import com.saaes.system.client.entity.AIAnalysisInfo;
import com.saaes.system.client.entity.AnalysisResult;
import com.saaes.system.client.entity.ScheduleEvent;
import com.saaes.system.service.AIAnalysisInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "AI分析结果")
@RestController
@RequestMapping("/api/analysis")
public class AIAnalysisInfoController {

    @Resource
    private AIAnalysisInfoService aiAnalysisInfoService;

    @Operation(description = "获取用户所有视频分析结果")
    @PostMapping("/query")
    public RestResponse<Page<AIAnalysisInfo>> queryContent(@RequestBody PageQuery<Map<String, Object>> pageQuery) {
        Page<AIAnalysisInfo> data = aiAnalysisInfoService.query(pageQuery);
        return RestResponse.success("查询成功", data);
    }

    @Operation(description = "获取单个视频分析结果")
    @GetMapping("/queryByTaskId")
    public RestResponse<AIAnalysisInfo> queryByTaskId(String taskId) {
        return RestResponse.success(aiAnalysisInfoService.queryByTaskId(taskId));
    }

    @Operation(description = "视频分析结果保存")
    @PostMapping("/save")
    public RestResponse<?> save(@RequestBody Map<String, Object> data) throws JsonProcessingException {
        boolean flag = aiAnalysisInfoService.save(data);
        return RestResponse.success("更新成功");
    }

    @Operation(description = "根据动作评估结果生成训练计划")
    @GetMapping("/generateTrainingPlan")
    public RestResponse<List<ScheduleEventDTO>> generateTrainingPlanWithAIAnalysisInfo(@RequestParam String historyId, @RequestParam(defaultValue = "7") String dayNum) throws NoApiKeyException, InputRequiredException {
        if (historyId == null || CommonUtil.isEmpty(historyId)){throw new MyException("历史记录ID不能为空");}
        return RestResponse.success("生成计划成功", aiAnalysisInfoService.generateTrainingPlanWithAIAnalysisInfo(historyId, dayNum));
    }

}
