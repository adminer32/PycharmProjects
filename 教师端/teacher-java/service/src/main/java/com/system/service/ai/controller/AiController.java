package com.system.service.ai.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.system.service.ai.service.AiService;
import com.system.service.ai.service.AiTeachingAdviceService;
import com.system.service.ai.vo.*;
import com.system.service.learning.service.LearningService;
import com.system.service.learning.vo.AiPlanSaveInput;
import com.system.web.RestResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "AI智能助手")
@RestController
@RequestMapping("/api/ai")
@SaCheckRole("TEACHER")
public class AiController {

    @Resource
    private AiService aiService;

    @Resource
    private LearningService learningService;

    @Resource
    private AiTeachingAdviceService adviceService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Operation(summary = "AI生成作业")
    @PostMapping("/generate-homework")
    public ResponseEntity<RestResponse<AiHomeworkResult>> generateHomework(
            @RequestBody AiGenerateHomeworkInput input) {

        try {
            int studentCount = input.getStudentCount() != null ? input.getStudentCount() : 30;
            String jsonResult = aiService.generateHomework(
                    input.getSubject(),
                    input.getDifficulty(),
                    studentCount,
                    input.getDeadline()
            );

            JsonNode root = objectMapper.readTree(jsonResult);
            AiHomeworkResult result = new AiHomeworkResult();
            result.setTitle(root.path("title").asText("AI生成的作业"));
            result.setRequirements(root.path("requirements").asText(""));
            result.setTasks(root.path("tasks"));
            result.setEvaluationCriteria(root.path("evaluationCriteria").asText(""));

            return ResponseEntity.ok(RestResponse.success("AI生成作业成功", result));

        } catch (Exception e) {
            RestResponse<AiHomeworkResult> errorResponse = new RestResponse<>();
            errorResponse.setHttpCode(500);
            errorResponse.setStatus("error");
            errorResponse.setMessage("AI生成失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @Operation(summary = "AI生成训练计划")
    @PostMapping("/generate-plan")
    public ResponseEntity<RestResponse<AiPlanResult>> generatePlan(
            @RequestBody AiGeneratePlanInput input) {

        try {
            int duration = input.getPlanDuration() != null ? input.getPlanDuration() : 4;
            String jsonResult = aiService.generateTrainingPlan(input.getStudentId(), duration);

            JsonNode root = objectMapper.readTree(jsonResult);
            AiPlanResult result = new AiPlanResult();
            result.setGoal(root.path("goal").asText("个性化训练计划"));
            result.setWeeklyPlans(root.path("weeklyPlans"));
            result.setMilestones(root.path("milestones"));
            result.setTips(root.path("tips").asText(""));

            return ResponseEntity.ok(RestResponse.success("AI生成训练计划成功", result));

        } catch (Exception e) {
            RestResponse<AiPlanResult> errorResponse = new RestResponse<>();
            errorResponse.setHttpCode(500);
            errorResponse.setStatus("error");
            errorResponse.setMessage("AI生成失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @Operation(summary = "保存AI生成的训练计划到数据库")
    @PostMapping("/save-plan")
    public ResponseEntity<RestResponse<String>> saveAiPlan(@RequestBody AiPlanSaveInput input) {
        try {
            Long teacherId = StpUtil.getLoginIdAsLong();
            input.setClassId(getClassIdFromToken());

            learningService.saveAiPlan(input);

            RestResponse<String> response = new RestResponse<>();
            response.setHttpCode(200);
            response.setStatus("success");
            response.setData(null);
            response.setMessage("计划保存成功，已拆分为" + input.getTotalWeeks() + "周记录");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            RestResponse<String> errorResponse = new RestResponse<>();
            errorResponse.setHttpCode(500);
            errorResponse.setStatus("error");
            errorResponse.setMessage("保存失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    private Long getClassIdFromToken() {
        Object classId = StpUtil.getSession().get("classId");
        if (classId instanceof Number) {
            return ((Number) classId).longValue();
        }
        return 1L;
    }

    @Operation(summary = "获取班级AI教学建议")
    @GetMapping("/advice/class/{classId}")
    public ResponseEntity<RestResponse<List<AiTeachingAdvice>>> getClassAdvice(@PathVariable Long classId) {
        try {
            List<AiTeachingAdvice> advices = adviceService.getClassAdvice(classId);
            RestResponse<List<AiTeachingAdvice>> response = new RestResponse<>();
            response.setHttpCode(200);
            response.setStatus("success");
            response.setData(advices);
            response.setMessage("获取成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            RestResponse<List<AiTeachingAdvice>> errorResponse = new RestResponse<>();
            errorResponse.setHttpCode(500);
            errorResponse.setStatus("error");
            errorResponse.setMessage("获取失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @Operation(summary = "重新生成班级AI教学建议")
    @PostMapping("/advice/class/{classId}/regenerate")
    public ResponseEntity<RestResponse<List<AiTeachingAdvice>>> regenerateClassAdvice(@PathVariable Long classId) {
        try {
            List<AiTeachingAdvice> advices = adviceService.regenerateClassAdvice(classId);
            RestResponse<List<AiTeachingAdvice>> response = new RestResponse<>();
            response.setHttpCode(200);
            response.setStatus("success");
            response.setData(advices);
            response.setMessage("AI教学建议已重新生成，共" + advices.size() + "条");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            RestResponse<List<AiTeachingAdvice>> errorResponse = new RestResponse<>();
            errorResponse.setHttpCode(500);
            errorResponse.setStatus("error");
            errorResponse.setMessage("重新生成失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @Operation(summary = "获取学生AI个性化改进建议")
    @GetMapping("/advice/student/{studentId}")
    public ResponseEntity<RestResponse<List<AiTeachingAdvice>>> getStudentAdvice(@PathVariable Long studentId) {
        try {
            List<AiTeachingAdvice> advices = adviceService.getStudentAdvice(studentId);
            RestResponse<List<AiTeachingAdvice>> response = new RestResponse<>();
            response.setHttpCode(200);
            response.setStatus("success");
            response.setData(advices);
            response.setMessage("获取成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            RestResponse<List<AiTeachingAdvice>> errorResponse = new RestResponse<>();
            errorResponse.setHttpCode(500);
            errorResponse.setStatus("error");
            errorResponse.setMessage("获取失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @Operation(summary = "重新生成学生AI个性化改进建议")
    @PostMapping("/advice/student/{studentId}/regenerate")
    public ResponseEntity<RestResponse<List<AiTeachingAdvice>>> regenerateStudentAdvice(@PathVariable Long studentId) {
        try {
            List<AiTeachingAdvice> advices = adviceService.regenerateStudentAdvice(studentId);
            RestResponse<List<AiTeachingAdvice>> response = new RestResponse<>();
            response.setHttpCode(200);
            response.setStatus("success");
            response.setData(advices);
            response.setMessage("AI个性化建议已重新生成，共" + advices.size() + "条");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            RestResponse<List<AiTeachingAdvice>> errorResponse = new RestResponse<>();
            errorResponse.setHttpCode(500);
            errorResponse.setStatus("error");
            errorResponse.setMessage("重新生成失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}
