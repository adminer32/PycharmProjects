package com.saaes.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.saaes.common.core.web.PageQuery;
import com.saaes.common.core.web.PageResult;
import com.saaes.common.core.web.RestResponse;
import com.saaes.system.client.entity.AIVideosHistory;
import com.saaes.system.service.AIVideosHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Array;
import java.util.List;
import java.util.Map;

@Tag(name = "AI视频分析历史记录")
@RestController
@RequestMapping("/api/video/history")
public class AIVideosHistoryController {

    @Resource
    private AIVideosHistoryService aiVideosHistoryService;

    @Operation(description = "历史记录查询")
    @PostMapping("/query")
    public RestResponse<Page<AIVideosHistory>> query(@RequestBody PageQuery<Map<String, Object>> pageQuery) {
        Page<AIVideosHistory> data = aiVideosHistoryService.query(pageQuery);
        return RestResponse.success("查询成功", data);
    }

    @Operation(description = "查询历史记录内容详情")
    @GetMapping("/detail")
    public RestResponse<?>queryHistoryDetail(Integer historyId) {
        Map<String, Object> data = aiVideosHistoryService.queryHistoryDetail(historyId);
        return RestResponse.success("查询成功", data);
    }

    @Operation(description = "记录保存")
    @PostMapping("/save")
    public RestResponse<?> save(@RequestBody AIVideosHistory aiVideosHistory) {
        return RestResponse.success("记录成功", aiVideosHistoryService.save(aiVideosHistory));
    }

    @Operation(description = "视频分析记录查询")
    @GetMapping("/queryAnalysisResult")
    public RestResponse<?> queryAnalysisResult(Integer id) {
        return RestResponse.success(aiVideosHistoryService.queryAnalysisResult(id));
    }

    @Operation(description = "视频分析记录批量删除")
    @DeleteMapping("/del")
    public RestResponse<?> del(@RequestParam List<Integer> ids) {
        aiVideosHistoryService.del(ids);
        return RestResponse.success("删除成功");
    }

}
