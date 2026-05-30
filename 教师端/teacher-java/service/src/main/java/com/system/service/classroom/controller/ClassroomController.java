package com.system.service.classroom.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.system.service.classroom.service.ClassroomService;
import com.system.service.classroom.vo.ClassroomVO;
import com.system.web.RestResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "课堂管理")
@RestController
@RequestMapping("/api/classroom")
public class ClassroomController {

    @Autowired
    private ClassroomService classroomService;

    @Operation(summary = "获取课程列表")
    @GetMapping("/lessons")
    public RestResponse<List<ClassroomVO.LessonItem>> getLessons(
            @RequestParam("classId") String classId) {
        Long teacherId = StpUtil.getLoginIdAsLong();
        return RestResponse.success(classroomService.getLessons(classId, teacherId));
    }

    @Operation(summary = "获取知识点列表")
    @GetMapping("/knowledge")
    public RestResponse<List<ClassroomVO.KnowledgePointVO>> getKnowledgePoints(
            @RequestParam("lessonId") Long lessonId) {
        return RestResponse.success(classroomService.getKnowledgePoints(lessonId));
    }

    @Operation(summary = "获取课堂笔记列表")
    @GetMapping("/notes")
    public RestResponse<List<ClassroomVO.NoteVO>> getNotes(
            @RequestParam("lessonId") Long lessonId) {
        return RestResponse.success(classroomService.getNotes(lessonId));
    }

    @Operation(summary = "添加知识点")
    @PostMapping("/knowledge")
    public RestResponse<ClassroomVO.KnowledgePointVO> addKnowledge(
            @RequestBody ClassroomVO.AddKnowledgeInput input) {
        return RestResponse.success(classroomService.addKnowledge(input));
    }

    @Operation(summary = "添加课堂笔记")
    @PostMapping("/note")
    public RestResponse<ClassroomVO.NoteVO> addNote(
            @RequestBody ClassroomVO.AddNoteInput input) {
        return RestResponse.success(classroomService.addNote(input));
    }

    @Operation(summary = "批量保存课堂笔记")
    @PostMapping("/notes/save")
    public RestResponse<Void> saveNotes(
            @RequestBody ClassroomVO.SaveNotesInput input) {
        classroomService.saveNotes(input);
        return RestResponse.success();
    }

    @Operation(summary = "分析课堂视频")
    @PostMapping("/analyze")
    public RestResponse<ClassroomVO.AnalyzeResult> analyzeVideo(
            @RequestBody ClassroomVO.AnalyzeInput input) {
        Long teacherId = StpUtil.getLoginIdAsLong();
        input.setTeacherId(teacherId);
        return RestResponse.success(classroomService.analyzeVideo(input));
    }

    @Operation(summary = "更新知识点视频")
    @PutMapping("/knowledge/{id}/video")
    public RestResponse<Void> updateKnowledgeVideo(
            @PathVariable("id") Long id,
            @RequestBody Map<String, String> body) {
        classroomService.updateKnowledgeVideo(id, body.get("videoUrl"));
        return RestResponse.success();
    }
}
