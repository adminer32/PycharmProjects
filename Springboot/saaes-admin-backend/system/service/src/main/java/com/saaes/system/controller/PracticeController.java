package com.saaes.system.controller;

import com.saaes.common.core.web.PageResult;
import com.saaes.common.core.web.RestResponse;
import com.saaes.system.client.dto.PracticeSubmissionDTO;
import com.saaes.system.client.dto.PracticeTaskQueryDTO;
import com.saaes.system.client.vo.PracticeTaskVO;
import com.saaes.system.service.PracticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "练习中心-学生端")
@RestController
@RequestMapping("/api/student/practices")
public class PracticeController {

    @Resource
    private PracticeService practiceService;

    @Operation(description = "分页获取学生的练习任务列表")
    @GetMapping
    public RestResponse<PageResult<PracticeTaskVO>> listMyTasks(PracticeTaskQueryDTO queryDTO) {
        return RestResponse.success(practiceService.queryStudentTasks(queryDTO));
    }

    @Operation(description = "首次提交练习视频")
    @PostMapping("/submissions")
    public RestResponse<?> submit(@Validated @RequestBody PracticeSubmissionDTO submissionDTO) {
        practiceService.createOrUpdatePractice(submissionDTO);
        return RestResponse.success();
    }

    @Operation(description = "修改已提交的练习视频")
    @PutMapping("/submissions")
    public RestResponse<?> resubmit(@Validated @RequestBody PracticeSubmissionDTO submissionDTO) {
        practiceService.createOrUpdatePractice(submissionDTO);
        return RestResponse.success();
    }

    @Operation(description = "获取练习详情")
    @GetMapping("/{taskId}")
    public RestResponse<PracticeTaskVO> getTaskDetail(@PathVariable Integer taskId) {
        return RestResponse.success(practiceService.getTaskDetail(taskId));
    }

    @Operation(description = "查看未读提醒")
    @GetMapping("/reminder/unread")
    public RestResponse<?> getUnread() {
        return RestResponse.success(practiceService.getUnreadReminders());
    }

    @Operation(description = "标记该次提醒为已读")
    @PutMapping("/reminder/read")
    public RestResponse<?> markRead(@RequestParam("reminderId") Integer reminderId) {practiceService.markReminderRead(reminderId);
        return RestResponse.success();
    }
}
