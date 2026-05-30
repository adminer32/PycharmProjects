package com.saaes.system.admin.practice.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.baomidou.mybatisplus.core.toolkit.Assert;
import com.saaes.common.core.validator.CreateGroup;
import com.saaes.common.core.validator.UpdateGroup;
import com.saaes.common.core.web.PageResult;
import com.saaes.common.core.web.RestResponse;
import com.saaes.system.admin.practice.dto.AdminPracticeFeedbackQueryDTO;
import com.saaes.system.admin.practice.service.AdminPracticeService;
import com.saaes.system.client.dto.PracticeFeedbackSaveDTO;
import com.saaes.system.client.vo.PracticeFeedbackVO;
import com.saaes.system.client.vo.PracticeTaskVO;
import com.saaes.system.admin.practice.dto.AdminPracticeTaskSaveDTO;
import com.saaes.system.admin.practice.dto.AdminPracticeTaskQueryDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "管理后台-练习任务管理")
@RestController
@RequestMapping("/api/admin/practices")
@SaCheckRole(value = { "super_admin", "teacher" }, mode = SaMode.OR)
public class AdminPracticeController {

    @Resource
    private AdminPracticeService adminPracticeService;

    @Operation(description = "老师分页获取练习任务列表")
    @GetMapping("/tasks")
    public RestResponse<PageResult<PracticeTaskVO>> listTasks(AdminPracticeTaskQueryDTO queryDTO) {
        return RestResponse.success(adminPracticeService.selectTaskPage(queryDTO));
    }

    @Operation(description = "发布练习任务")
    @PostMapping("/tasks")
    public RestResponse<?> publish(@Validated(CreateGroup.class) @RequestBody AdminPracticeTaskSaveDTO saveDTO) {
        adminPracticeService.createOrUpdateTask(saveDTO);
        return RestResponse.success();
    }

    @Operation(description = "修改练习任务")
    @PutMapping("/tasks/{id}")
    public RestResponse<?> update(@Validated(UpdateGroup.class) @RequestBody AdminPracticeTaskSaveDTO saveDTO) {
        adminPracticeService.createOrUpdateTask(saveDTO);
        return RestResponse.success();
    }

    @Operation(description = "删除练习任务")
    @DeleteMapping("/tasks")
    public RestResponse<?> delete(@RequestParam("ids") List<Integer> ids) {
        Assert.notEmpty(ids, "待删除的练习任务ids列表不能为空");
        List<Integer> distinctIds = ids.stream().distinct().collect(Collectors.toList());
        adminPracticeService.deleteTasks(distinctIds);
        return RestResponse.success();
    }

    @Operation(description = "分页获取学生提交练习任务列表")
    @GetMapping("/submissions")
    public RestResponse<PageResult<PracticeFeedbackVO>> listFeedbacks(AdminPracticeFeedbackQueryDTO queryDTO) {
        return RestResponse.success(adminPracticeService.selectFeedbacksPage(queryDTO));
    }
    @Operation(description = "首次评价学生练习")
    @PostMapping("/submissions/feedback")
    public RestResponse<?> gradeSubmission(@Validated @RequestBody PracticeFeedbackSaveDTO feedbackDTO) {
        adminPracticeService.createOrUpdateFeedback(feedbackDTO);
        return RestResponse.success();
    }

    @Operation(description = "修改已有评价")
    @PutMapping("/submissions/{submissionId}/feedback")
    public RestResponse<?> updateFeedback(@PathVariable Integer submissionId, @Validated @RequestBody PracticeFeedbackSaveDTO feedbackDTO) {
        feedbackDTO.setSubmissionId(submissionId);
        adminPracticeService.createOrUpdateFeedback(feedbackDTO);
        return RestResponse.success();
    }

    @Operation(description = "获取学生练习提交详情")
    @GetMapping("/submissions/{submissionId}")
    public RestResponse<PracticeFeedbackVO> getSubmissionDetail(@PathVariable Integer submissionId) {
        return RestResponse.success(adminPracticeService.getSubmissionDetail(submissionId));
    }

    @Operation(description = "一键提醒所有未完成学生")
    @PostMapping("/tasks/{taskId}/reminder")
    public RestResponse<?> remindStudents(@PathVariable Integer taskId) {
        adminPracticeService.remindUnfinishedStudents(taskId);
        return RestResponse.success();
    }
}
