package com.system.service.ai.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.system.service.ai.client.DeepSeekClient;
import com.system.service.ai.mapper.AiMapper;
import com.system.service.ai.prompt.PromptTemplates;
import com.system.service.ai.service.AiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AiServiceImpl implements AiService {

    @Autowired
    private DeepSeekClient deepSeekClient;

    @Autowired
    private AiMapper aiMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String generateHomework(String subject, String difficulty, int studentCount, String deadline) {
        log.info("AI生成作业 - 主题:{} 难度:{} 人数:{}", subject, difficulty, studentCount);

        try {
            String systemPrompt = PromptTemplates.getHomeworkSystemPrompt();
            String userPrompt = PromptTemplates.getHomeworkUserPrompt(subject, difficulty, studentCount, deadline);

            String response = deepSeekClient.chat(systemPrompt, userPrompt);
            return cleanJsonResponse(response);

        } catch (Exception e) {
            log.error("生成作业失败", e);
            throw new RuntimeException("AI生成作业失败: " + e.getMessage());
        }
    }

    @Override
    public String generateTrainingPlan(Long studentId, int planDuration) {
        log.info("AI生成训练计划 - 学生ID:{} 周数:{}", studentId, planDuration);

        try {
            Map<String, Object> studentInfo = aiMapper.getStudentBasicInfo(studentId);
            if (studentInfo == null || studentInfo.isEmpty()) {
                throw new RuntimeException("未找到学生信息");
            }

            String studentName = (String) studentInfo.getOrDefault("name", "未知学生");
            String currentLevel = (String) studentInfo.getOrDefault("level", "初级");
            String goal = (String) studentInfo.getOrDefault("goal", "提升毽球综合技能");

            List<Map<String, Object>> weaknessList = aiMapper.getStudentWeakness(studentId);
            StringBuilder weaknessAnalysis = new StringBuilder();
            if (weaknessList != null && !weaknessList.isEmpty()) {
                for (Map<String, Object> weakness : weaknessList) {
                    weaknessAnalysis.append("- ")
                            .append(weakness.getOrDefault("skill_type", "未知技能"))
                            .append(": 当前得分 ").append(weakness.getOrDefault("personal_score", 0))
                            .append("分（班级平均 ").append(weakness.getOrDefault("class_avg_score", 0)).append("分）\n");
                }
            } else {
                weaknessAnalysis.append("暂无明显弱项，继续保持当前水平即可。");
            }

            List<Map<String, Object>> recentActivity = aiMapper.getStudentRecentActivity(studentId);
            StringBuilder performanceBuilder = new StringBuilder();
            if (recentActivity != null && !recentActivity.isEmpty()) {
                for (Map<String, Object> activity : recentActivity) {
                    performanceBuilder.append("- ")
                            .append(activity.getOrDefault("date", ""))
                            .append(": ").append(activity.getOrDefault("action_type", ""))
                            .append(" 得分").append(activity.getOrDefault("score", "")).append("\n");
                }
            } else {
                performanceBuilder.append("近期暂无学习记录。");
            }

            String systemPrompt = PromptTemplates.getPlanSystemPrompt();
            String userPrompt = PromptTemplates.getPlanUserPrompt(
                    studentName, currentLevel, goal,
                    weaknessAnalysis.toString(), performanceBuilder.toString(), planDuration
            );

            String response = deepSeekClient.chat(systemPrompt, userPrompt);
            return cleanJsonResponse(response);

        } catch (Exception e) {
            log.error("生成训练计划失败", e);
            throw new RuntimeException("AI生成训练计划失败: " + e.getMessage());
        }
    }

    private String cleanJsonResponse(String response) {
        if (response == null || response.isBlank()) {
            return "{}";
        }

        String cleaned = response.trim();

        if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(cleaned.indexOf('\n') + 1);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3).trim();
        }

        if (cleaned.toLowerCase().startsWith("json")) {
            cleaned = cleaned.substring(4).trim();
        }

        return cleaned;
    }
}
