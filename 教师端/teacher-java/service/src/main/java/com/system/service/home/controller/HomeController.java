package com.system.service.home.controller;

import com.system.web.RestResponse;
import com.system.service.home.service.HomeService;
import com.system.service.home.vo.HomeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "首页数据")
@RestController
@RequestMapping("/api/home")
public class HomeController {

    @Autowired
    private HomeService homeService;

    @Operation(summary = "获取班级概览")
    @GetMapping("/overview")
    public RestResponse<HomeVO.Overview> getOverview(
            @RequestParam(value = "classId", required = false) String classId) {
        if (classId == null || classId.isEmpty()) classId = "0";
        return RestResponse.success(homeService.getHomeOverview(classId));
    }

    @Operation(summary = "获取待处理事项")
    @GetMapping("/pending")
    public RestResponse<List<HomeVO.PendingItem>> getPendingItems(
            @RequestParam("teacherId") Long teacherId,
            @RequestParam(value = "classId", required = false) String classId) {
        return RestResponse.success(homeService.getPendingItems(teacherId, classId));
    }

    @Operation(summary = "获取通知列表")
    @GetMapping("/notices")
    public RestResponse<Map<String, Object>> getNotices(
            @RequestParam("classId") String classId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "5") int size) {
        return RestResponse.success(homeService.getNotices(classId, page, size));
    }

    @Operation(summary = "发布通知")
    @PostMapping("/notice")
    public RestResponse<HomeVO.Notice> publishNotice(@RequestBody HomeVO.NoticeInput input) {
        return RestResponse.success(homeService.saveNotice(input));
    }

    @Operation(summary = "编辑通知")
    @PutMapping("/notice")
    public RestResponse<HomeVO.Notice> updateNotice(@RequestBody HomeVO.NoticeInput input) {
        return RestResponse.success(homeService.updateNotice(input));
    }

    @Operation(summary = "删除通知")
    @DeleteMapping("/notice/{id}")
    public RestResponse<Boolean> deleteNotice(@PathVariable("id") Long id) {
        return RestResponse.success(homeService.deleteNotice(id));
    }
}
