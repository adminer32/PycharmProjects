package com.saaes.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaIgnore;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.aliyuncs.exceptions.ClientException;
import com.saaes.common.core.utils.TranscriptionTaskUtils;
import com.saaes.common.core.web.MyException;
import com.saaes.common.core.web.PageQuery;
import com.saaes.common.core.web.PageResult;
import com.saaes.common.core.web.RestResponse;
import com.saaes.system.client.entity.AiNoteInfo;
import com.saaes.system.client.entity.ChapterInfo;
import com.saaes.system.client.entity.CourseCategory;
import com.saaes.system.service.AiService;
import com.saaes.system.service.ChapterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "AI任务")
@RestController
@RequestMapping("/api/ai")
public class AiController {

    @Resource
    private AiService aiService;

    @SaIgnore
    @Operation(description = "新建个人视频笔记分析任务")
    @PostMapping("/addTranscriptionTask")
    public RestResponse<AiNoteInfo> addTranscriptionTask(@RequestBody Map<String, Object> params) {
        return RestResponse.success("任务添加成功", aiService.personalAdd(params));
    }


    @SaIgnore
    @Operation(description = "更新个人笔记内容")
    @PostMapping("/updateNote")
    public RestResponse<AiNoteInfo> updateNote(@RequestBody Map<String, Object> params) {
        return RestResponse.success("更新成功", aiService.personalNoteUpdate(params));
    }

    @SaIgnore
    @Operation(description = "个人笔记润色")
    @PostMapping("/NotePolishing")
    public RestResponse<?> NotePolishing(@RequestBody Map<String, Object> params) throws NoApiKeyException, InputRequiredException {
        if (params == null || params.get("message") == null) {throw new MyException("参数不能为空");}
        return RestResponse.success("润色成功", aiService.queryQwenTurbo(Map.of("message", "帮我合理的润色一下用户输入的文字，且只返回润色后的存文字，不需要markdown格式：" + params.get("message").toString())));
    }

    @SaIgnore
    @Operation(description = "查询笔记识别结果")
    @GetMapping("/queryTranscriptionTask")
    public RestResponse<?> queryTranscriptionTask(@RequestParam(name = "TaskId") String TaskId) throws ClientException {
        return RestResponse.success("查询成功",aiService.query(TaskId));
    }

    @SaIgnore
    @Operation(description = "根据任务ID查询笔记识别结果")
    @GetMapping("/queryTaskStatus")
    public RestResponse<AiNoteInfo> queryTranscriptionTaskById(@RequestParam(name = "taskId") String TaskId) throws ClientException {
        return RestResponse.success("查询成功", aiService.queryByTaskId(TaskId));
    }

    @Operation(description = "笔记任务批量删除")
    @DeleteMapping("/del")
    public RestResponse<?> del(@RequestParam List<Integer> ids) {
        return RestResponse.success(aiService.del(ids));
    }

    @SaIgnore
    @Operation(description = "查询个人视频笔记")
    @PostMapping("/queryPersonalNotes")
    public RestResponse<PageResult<AiNoteInfo>> queryPersonalNotes(@RequestBody PageQuery<Map<String, Object>> pageQuery) {
        PageResult<AiNoteInfo> pageResult = aiService.queryPersonalNotes(pageQuery);
        return RestResponse.success("查询成功", pageResult);
    }

    @SaIgnore
    @Operation(description = "Qwen大模型输入")
    @PostMapping("/callQwenMaxLatest")
    public RestResponse<?> callQwenMaxLatest(@RequestBody Map<String, Object> params) throws NoApiKeyException, InputRequiredException {
        return RestResponse.success("查询成功",aiService.queryQwen(params));
    }
}
