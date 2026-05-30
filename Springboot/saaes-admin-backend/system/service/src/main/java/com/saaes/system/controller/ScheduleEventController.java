package com.saaes.system.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.saaes.common.core.web.RestResponse;
import com.saaes.system.client.entity.ScheduleEvent;
import com.saaes.system.service.ScheduleEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;


@Tag(name = "训练计划")
@RestController
@RequestMapping("/api/schedules")
public class ScheduleEventController {

    @Autowired
    private ScheduleEventService scheduleEventService;

    @Operation(description = "获取某月或某天事件数据")
    @GetMapping("/query")
    public RestResponse<Map<String, List<Map<String, Object>>>> getGroupedEventsByMonth(@RequestParam String date) {
        long loginId = StpUtil.getLoginIdAsLong();
        Map<String, List<Map<String, Object>>> grouped = scheduleEventService.getGroupedEventsByDate(date, loginId);
        return RestResponse.success("查询成功", grouped);
    }

    @Operation(description = "保存和更新事件数据")
    @PostMapping("/save")
    public RestResponse<ScheduleEvent> save(@RequestBody ScheduleEvent event) {
        return RestResponse.success("保存成功", scheduleEventService.saveOrUpdateEvent(event));
    }

    @Operation(description = "批量保存训练事件数据")
    @PostMapping("/batch-save")
    public RestResponse<List<ScheduleEvent>> batchSave(@RequestBody List<ScheduleEvent> events) {
        boolean success = scheduleEventService.saveBatch(events);
        return RestResponse.success("批量保存" + (success ? "成功" : "失败"), events);
    }

    @Operation(description = "批量删除事件数据")
    @DeleteMapping("/del")
    @Transactional
    public RestResponse<?> deleteBatch(@RequestParam List<Long> ids) {
        scheduleEventService.deleteBatchEvents(ids);
        return RestResponse.success("删除成功");
    }
}
