package com.system.service.learning.controller;

import com.system.service.learning.service.ActionVideoService;
import com.system.service.learning.service.LearningService;
import com.system.service.learning.vo.ActionVideoVO;
import com.system.service.learning.vo.LearningVO;
import com.system.web.RestResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "学习管理")
@RestController
@RequestMapping("/api/learning")
public class LearningController {

    @Autowired
    private LearningService learningService;

    @Autowired
    private ActionVideoService actionVideoService;

    @Operation(summary = "获取学习概览")
    @GetMapping("/overview")
    public RestResponse<LearningVO.Overview> getOverview(
            @RequestParam("classId") String classId) {
        return RestResponse.success(learningService.getOverview(classId));
    }

    @Operation(summary = "获取学生列表")
    @GetMapping("/students")
    public RestResponse<List<LearningVO.StudentInfo>> getStudentList(
            @RequestParam("classId") String classId,
            @RequestParam(value = "keyword", required = false) String keyword) {
        return RestResponse.success(learningService.getStudentList(classId, keyword));
    }

    @Operation(summary = "获取训练计划详情")
    @GetMapping("/plan")
    public RestResponse<LearningVO.PlanDetail> getPlanByStudentId(
            @RequestParam("studentId") Long studentId) {
        return RestResponse.success(learningService.getCurrentWeekPlan(studentId));
    }

    @Operation(summary = "获取当前周计划")
    @GetMapping("/plan/current-week")
    public RestResponse<LearningVO.PlanDetail> getCurrentWeekPlan(
            @RequestParam("studentId") Long studentId) {
        return RestResponse.success(learningService.getCurrentWeekPlan(studentId));
    }

    @Operation(summary = "获取指定周的计划")
    @GetMapping("/plan/week")
    public RestResponse<LearningVO.PlanDetail> getWeekPlanByNumber(
            @RequestParam("studentId") Long studentId,
            @RequestParam("weekNumber") Integer weekNumber) {
        return RestResponse.success(learningService.getWeekPlanByNumber(studentId, weekNumber));
    }

    @Operation(summary = "获取学生所有周计划（历史）")
    @GetMapping("/plan/all")
    public RestResponse<List<Map<String, Object>>> getAllPlans(
            @RequestParam("studentId") Long studentId) {
        return RestResponse.success(learningService.getStudentAllPlans(studentId));
    }

    @Operation(summary = "获取学生的计划组列表")
    @GetMapping("/plan/groups")
    public RestResponse<List<Map<String, Object>>> getPlanGroups(
            @RequestParam("studentId") Long studentId) {
        return RestResponse.success(learningService.getStudentPlanGroups(studentId));
    }

    @Operation(summary = "审批训练计划")
    @PutMapping("/plan/review")
    public RestResponse<Boolean> reviewPlan(@RequestBody LearningVO.ReviewInput input) {
        return RestResponse.success(learningService.reviewPlan(input));
    }

    @Operation(summary = "获取动作视频列表")
    @GetMapping("/videos")
    public RestResponse<List<ActionVideoVO.VideoInfo>> getVideoList(
            @RequestParam(value = "classId", required = false) String classId,
            @RequestParam(value = "category", required = false) String category) {
        return RestResponse.success(actionVideoService.getVideoList(classId, category));
    }

    @Operation(summary = "添加动作视频")
    @PostMapping("/videos")
    public RestResponse<ActionVideoVO.VideoInfo> addVideo(@RequestBody ActionVideoVO.AddVideoInput input) {
        return RestResponse.success(actionVideoService.addVideo(input));
    }

    @Operation(summary = "推荐动作视频给学生")
    @PostMapping("/videos/recommend")
    public RestResponse<Boolean> recommendVideo(@RequestBody ActionVideoVO.RecommendInput input) {
        return RestResponse.success(actionVideoService.recommendVideo(input));
    }

    @Operation(summary = "删除学生的某个计划组")
    @DeleteMapping("/plan/group")
    public RestResponse<Boolean> deletePlanGroup(
            @RequestParam("studentId") Long studentId,
            @RequestParam("planGroupId") Long planGroupId) {
        learningService.deletePlanGroup(studentId, planGroupId);
        return RestResponse.success(true);
    }
}
