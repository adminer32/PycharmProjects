package com.system.service.homework.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.system.service.homework.dto.CreateHomeworkDTO;
import com.system.service.homework.dto.GradeSubmissionDTO;
import com.system.service.homework.vo.HomeworkDetailVO;
import com.system.service.homework.vo.HomeworkListVO;
import com.system.service.homework.vo.SubmissionVO;
import com.system.service.homework.service.HomeworkService;
import com.system.web.RestResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "作业管理接口")
@RestController
@RequestMapping("/api/homework")
@SaCheckRole("TEACHER")
public class HomeworkController {

    @Resource
    private HomeworkService homeworkService;

    @Operation(description = "获取作业列表")
    @GetMapping("/list")
    public RestResponse<List<HomeworkListVO>> getHomeworkList(
            @RequestParam(required = false) Long classId) {
        Long teacherId = StpUtil.getLoginIdAsLong();
        return RestResponse.success(homeworkService.getHomeworkList(teacherId, classId));
    }

    @Operation(description = "获取作业详情")
    @GetMapping("/{id}")
    public RestResponse<HomeworkDetailVO> getHomeworkDetail(@PathVariable Long id) {
        return RestResponse.success(homeworkService.getHomeworkDetail(id));
    }

    @Operation(description = "发布新作业")
    @PostMapping
    public RestResponse<Long> createHomework(@RequestBody @Valid CreateHomeworkDTO dto) {
        Long teacherId = StpUtil.getLoginIdAsLong();
        return RestResponse.success("发布成功", homeworkService.createHomework(dto, teacherId));
    }

    @Operation(description = "更新作业信息")
    @PutMapping("/{id}")
    public RestResponse<?> updateHomework(@PathVariable Long id, @RequestBody @Valid CreateHomeworkDTO dto) {
        homeworkService.updateHomework(id, dto);
        return RestResponse.success("更新成功");
    }

    @Operation(description = "删除作业")
    @DeleteMapping("/{id}")
    public RestResponse<?> deleteHomework(@PathVariable Long id) {
        homeworkService.deleteHomework(id);
        return RestResponse.success("删除成功");
    }

    @Operation(description = "获取学生提交列表")
    @GetMapping("/{homeworkId}/submissions")
    public RestResponse<List<SubmissionVO>> getSubmissions(@PathVariable Long homeworkId) {
        return RestResponse.success(homeworkService.getSubmissions(homeworkId));
    }

    @Operation(description = "获取提交详情")
    @GetMapping("/submission/{submissionId}")
    public RestResponse<SubmissionVO> getSubmissionDetail(@PathVariable Long submissionId) {
        return RestResponse.success(homeworkService.getSubmissionDetail(submissionId));
    }

    @Operation(description = "批改作业")
    @PutMapping("/submission/{submissionId}/grade")
    public RestResponse<?> gradeSubmission(@PathVariable Long submissionId, @RequestBody @Valid GradeSubmissionDTO dto) {
        homeworkService.gradeSubmission(submissionId, dto);
        return RestResponse.success("批改成功");
    }
}
