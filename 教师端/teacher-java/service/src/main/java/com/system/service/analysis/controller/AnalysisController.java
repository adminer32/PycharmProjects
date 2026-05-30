package com.system.service.analysis.controller;

import com.system.web.RestResponse;
import com.system.service.analysis.service.AnalysisService;
import com.system.service.analysis.entity.MotionAnalysisData;
import com.system.service.analysis.entity.StudentPhysique;
import com.system.service.analysis.vo.AnalysisVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;

@Tag(name = "学情分析管理接口")
@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {

    @Resource
    private AnalysisService analysisService;

    @Operation(summary = "获取班级学情概览")
    @GetMapping("/class/{classId}/dashboard")
    public RestResponse<AnalysisVO.ClassDashboard> getClassDashboard(
            @PathVariable("classId") Long classId,
            @RequestParam(value = "month", required = false) String month) {
        return RestResponse.success(analysisService.getClassDashboard(classId, month));
    }

    @Operation(summary = "获取学生学情概览")
    @GetMapping("/student/{studentId}/dashboard")
    public RestResponse<AnalysisVO.StudentDashboard> getStudentDashboard(
            @PathVariable("studentId") Long studentId,
            @RequestParam(value = "month", required = false) String month) {
        return RestResponse.success(analysisService.getStudentDashboard(studentId, month));
    }

    @Operation(summary = "获取班级学生列表")
    @GetMapping("/class/{classId}/students")
    public RestResponse<List<Map<String, Object>>> getStudents(@PathVariable("classId") Long classId) {
        return RestResponse.success(analysisService.getStudentsByClass(classId));
    }

    @Operation(summary = "获取学生身体分析数据")
    @GetMapping("/student/{studentId}/motion-analysis")
    public RestResponse<MotionAnalysisData> getMotionAnalysisData(@PathVariable("studentId") Integer studentId) {
        MotionAnalysisData data = analysisService.getMotionAnalysisData(studentId);
        if (data == null) {
            data = new MotionAnalysisData();
        }
        return RestResponse.success(data);
    }

    @Operation(summary = "获取学生体质数据")
    @GetMapping("/student/{studentId}/physique")
    public RestResponse<StudentPhysique> getStudentPhysique(@PathVariable("studentId") Integer studentId) {
        StudentPhysique data = analysisService.getStudentPhysique(studentId);
        if (data == null) {
            data = new StudentPhysique();
        }
        return RestResponse.success(data);
    }
}
