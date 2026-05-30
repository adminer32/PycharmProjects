package com.system.service.ai.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.system.service.ai.client.DeepSeekClient;
import com.system.service.ai.mapper.AiTeachingAdviceMapper;
import com.system.service.ai.mapper.AiMapper;
import com.system.service.ai.prompt.PromptTemplates;
import com.system.service.ai.service.AiTeachingAdviceService;
import com.system.service.ai.vo.AiTeachingAdvice;
import com.system.service.analysis.mapper.AnalysisMapper;
import com.system.service.analysis.vo.AnalysisVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AiTeachingAdviceServiceImpl implements AiTeachingAdviceService {

    @Autowired
    private AiTeachingAdviceMapper adviceMapper;

    @Autowired
    private DeepSeekClient deepSeekClient;

    @Autowired
    private AnalysisMapper analysisMapper;

    @Autowired
    private AiMapper aiMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<AiTeachingAdvice> getClassAdvice(Long classId) {
        return adviceMapper.selectByTarget("CLASS", classId);
    }

    @Override
    public List<AiTeachingAdvice> getStudentAdvice(Long studentId) {
        return adviceMapper.selectByTarget("STUDENT", studentId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<AiTeachingAdvice> regenerateClassAdvice(Long classId) {
        log.info("重新生成班级AI教学建议 - 班级ID: {}", classId);

        try {
            String className = getClassName(classId);
            int studentCount = getTotalStudents(classId);

            LocalDate now = LocalDate.now();
            String startOfMonth = now.with(TemporalAdjusters.firstDayOfMonth()).toString();
            String endOfMonth = now.with(TemporalAdjusters.lastDayOfMonth()).toString();

            BigDecimal checkinRate = getClassCheckinRate(classId, startOfMonth, endOfMonth);
            int qualifiedCount = getQualifiedCount(classId, startOfMonth, endOfMonth);

            String scoreDistribution = buildScoreDistribution(classId);
            String actionDistribution = buildActionDistribution(classId);
            String weaknessSummary = buildWeaknessSummary(classId);

            String systemPrompt = PromptTemplates.getClassAdviceSystemPrompt();
            String userPrompt = PromptTemplates.getClassAdviceUserPrompt(
                    className, studentCount,
                    scoreDistribution, actionDistribution,
                    checkinRate.doubleValue(), qualifiedCount,
                    weaknessSummary
            );

            log.info("调用DeepSeek生成班级教学建议...");
            String response = deepSeekClient.chat(systemPrompt, userPrompt);
            String cleanedJson = cleanJsonResponse(response);

            List<AiTeachingAdvice> advices = parseAdvicesFromJson(cleanedJson, "CLASS", classId);

            adviceMapper.deleteByTarget("CLASS", classId);
            if (!advices.isEmpty()) {
                adviceMapper.batchInsert(advices);
                log.info("班级AI教学建议已保存，共{}条", advices.size());
            }

            return advices;

        } catch (Exception e) {
            log.error("重新生成班级AI教学建议失败", e);
            throw new RuntimeException("AI生成教学建议失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<AiTeachingAdvice> regenerateStudentAdvice(Long studentId) {
        log.info("重新生成学生AI个性化改进建议 - 学生ID: {}", studentId);

        try {
            Map<String, Object> studentInfo = aiMapper.getStudentBasicInfo(studentId);
            if (studentInfo == null || studentInfo.isEmpty()) {
                throw new RuntimeException("未找到学生信息");
            }

            String studentName = (String) studentInfo.getOrDefault("name", "未知学生");
            String currentLevel = (String) studentInfo.getOrDefault("level", "初级");

            String skillData = buildStudentSkillData(studentId);
            String checkinInfo = buildStudentCheckinInfo(studentId);
            String recentPerformance = buildStudentRecentPerformance(studentId);

            String systemPrompt = PromptTemplates.getStudentAdviceSystemPrompt();
            String userPrompt = PromptTemplates.getStudentAdviceUserPrompt(
                    studentName, currentLevel,
                    skillData, checkinInfo, recentPerformance
            );

            log.info("调用DeepSeek生成学生个性化建议...");
            String response = deepSeekClient.chat(systemPrompt, userPrompt);
            String cleanedJson = cleanJsonResponse(response);

            List<AiTeachingAdvice> advices = parseAdvicesFromJson(cleanedJson, "STUDENT", studentId);

            adviceMapper.deleteByTarget("STUDENT", studentId);
            if (!advices.isEmpty()) {
                adviceMapper.batchInsert(advices);
                log.info("学生AI个性化改进建议已保存，共{}条", advices.size());
            }

            return advices;

        } catch (Exception e) {
            log.error("重新生成学生AI个性化改进建议失败", e);
            throw new RuntimeException("AI生成个性化建议失败: " + e.getMessage());
        }
    }

    private String getClassName(Long classId) {
        Map<String, Object> classInfo = analysisMapper.getClassName(classId);
        if (classInfo != null && classInfo.containsKey("name")) {
            return (String) classInfo.get("name");
        }
        return "默认班级";
    }

    private int getTotalStudents(Long classId) {
        Integer count = analysisMapper.getTotalClassStudents(classId);
        return count != null ? count : 0;
    }

    private BigDecimal getClassCheckinRate(Long classId, String startDate, String endDate) {
        Integer uniqueCheckins = analysisMapper.getUniqueCheckinStudentsInPeriod(classId, startDate, endDate);
        Integer totalStudents = analysisMapper.getTotalClassStudents(classId);
        if (uniqueCheckins == null) uniqueCheckins = 0;
        if (totalStudents == null || totalStudents == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(uniqueCheckins * 100.0 / totalStudents).setScale(1, RoundingMode.HALF_UP);
    }

    private int getQualifiedCount(Long classId, String startDate, String endDate) {
        Integer count = analysisMapper.getQualifiedCheckinStudents(classId, startDate, endDate, 12);
        return count != null ? count : 0;
    }

    private String buildScoreDistribution(Long classId) {
        List<Map<String, Object>> distList = analysisMapper.getClassHomeworkScoreDistribution(classId);
        StringBuilder sb = new StringBuilder();
        if (distList != null && !distList.isEmpty()) {
            Map<String, Object> row = distList.get(0);
            sb.append("- 优秀（≥90分）: ").append(row.getOrDefault("excellent", 0)).append("人\n");
            sb.append("- 良好（75-89分）: ").append(row.getOrDefault("good", 0)).append("人\n");
            sb.append("- 及格（60-74分）: ").append(row.getOrDefault("pass", 0)).append("人\n");
            sb.append("- 不及格（<60分）: ").append(row.getOrDefault("fail", 0)).append("人\n");
        } else {
            sb.append("暂无成绩分布数据");
        }
        return sb.toString();
    }

    private String buildActionDistribution(Long classId) {
        List<AnalysisVO.SkillScore> radarData = analysisMapper.getClassRadarData(classId);
        StringBuilder sb = new StringBuilder();
        if (radarData != null && !radarData.isEmpty()) {
            for (AnalysisVO.SkillScore item : radarData) {
                String skillType = item.getSkillType();
                Number avgScore = item.getClassAvgScore();
                sb.append("- ").append(skillType).append(": 班级平均 ").append(avgScore).append("分\n");
            }
        } else {
            sb.append("暂无动作能力数据");
        }
        return sb.toString();
    }

    private String buildWeaknessSummary(Long classId) {
        List<AnalysisVO.SkillScore> radarData = analysisMapper.getClassRadarData(classId);
        if (radarData == null || radarData.isEmpty()) return null;

        radarData.sort((a, b) -> {
            Number scoreA = a.getClassAvgScore() != null ? a.getClassAvgScore() : 0;
            Number scoreB = b.getClassAvgScore() != null ? b.getClassAvgScore() : 0;
            return Double.compare(scoreA.doubleValue(), scoreB.doubleValue());
        });

        StringBuilder sb = new StringBuilder();
        sb.append("班级最薄弱的 3 项技能：\n");
        int count = Math.min(3, radarData.size());
        for (int i = 0; i < count; i++) {
            AnalysisVO.SkillScore item = radarData.get(i);
            String skillType = item.getSkillType();
            Number avgScore = item.getClassAvgScore();
            sb.append((i + 1)).append(". ").append(skillType).append("（平均").append(avgScore).append("分）\n");
        }
        return sb.toString();
    }

    private String buildStudentSkillData(Long studentId) {
        List<AnalysisVO.SkillScore> radarData = analysisMapper.getStudentRadarData(studentId);
        StringBuilder sb = new StringBuilder();
        if (radarData != null && !radarData.isEmpty()) {
            for (AnalysisVO.SkillScore item : radarData) {
                String skillType = item.getSkillType();
                Number personalScore = item.getPersonalScore();
                Number classAvgScore = item.getClassAvgScore();
                sb.append("- ").append(skillType)
                  .append(": 个人得分 ").append(personalScore)
                  .append("分（班级平均 ").append(classAvgScore).append("分）\n");
            }
        } else {
            sb.append("暂无技能数据");
        }
        return sb.toString();
    }

    private String buildStudentCheckinInfo(Long studentId) {
        LocalDate now = LocalDate.now();
        String currentMonthStr = now.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        String startOfMonth = now.with(TemporalAdjusters.firstDayOfMonth()).toString();
        String endOfMonth = now.with(TemporalAdjusters.lastDayOfMonth()).toString();

        Integer checkinDays = analysisMapper.getStudentCheckinCountInPeriod(studentId, startOfMonth, endOfMonth);
        if (checkinDays == null) checkinDays = 0;

        int daysInMonth = now.lengthOfMonth();
        double rate = daysInMonth > 0 ? (checkinDays * 100.0 / daysInMonth) : 0;

        StringBuilder sb = new StringBuilder();
        sb.append("- 本月打卡天数: ").append(checkinDays).append("天\n");
        sb.append("- 本月出勤率: ").append(BigDecimal.valueOf(rate).setScale(1, RoundingMode.HALF_UP)).append("%\n");

        List<Integer> checkinDaysList = analysisMapper.getStudentCheckindays(studentId, currentMonthStr);
        if (checkinDaysList != null && !checkinDaysList.isEmpty()) {
            sb.append("- 具体打卡日期: 第").append(checkinDaysList.stream().map(String::valueOf).reduce((a, b) -> a + ", " + b).orElse("")).append("天\n");
        }
        return sb.toString();
    }

    private String buildStudentRecentPerformance(Long studentId) {
        List<Map<String, Object>> recentActivity = aiMapper.getStudentRecentActivity(studentId);
        StringBuilder sb = new StringBuilder();
        if (recentActivity != null && !recentActivity.isEmpty()) {
            for (Map<String, Object> activity : recentActivity) {
                sb.append("- ")
                  .append(activity.getOrDefault("date", ""))
                  .append(": ").append(activity.getOrDefault("action_type", ""))
                  .append(" 得分").append(activity.getOrDefault("score", "")).append("\n");
            }
        } else {
            sb.append("近期暂无学习记录");
        }
        return sb.toString();
    }

    private List<AiTeachingAdvice> parseAdvicesFromJson(String jsonStr, String targetType, Long targetId) {
        List<AiTeachingAdvice> result = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(jsonStr);
            if (root.isArray()) {
                for (JsonNode node : root) {
                    AiTeachingAdvice advice = new AiTeachingAdvice();
                    advice.setTargetType(targetType);
                    advice.setTargetId(targetId);

                    String priority = node.path("priority").asText("MEDIUM");
                    if ("HIGH".equals(priority) || "MEDIUM".equals(priority) || "LOW".equals(priority)) {
                        advice.setPriorityLevel(priority);
                    } else {
                        advice.setPriorityLevel("MEDIUM");
                    }

                    advice.setTitle(node.path("title").asText(""));
                    advice.setDescription(node.path("description").asText(""));

                    JsonNode suggestionsNode = node.path("suggestions");
                    if (suggestionsNode.isArray()) {
                        List<String> suggestionsList = new ArrayList<>();
                        for (JsonNode s : suggestionsNode) {
                            suggestionsList.add(s.asText());
                        }
                        advice.setSuggestions(objectMapper.writeValueAsString(suggestionsList));
                    } else {
                        advice.setSuggestions("[]");
                    }

                    result.add(advice);
                }
            }
        } catch (Exception e) {
            log.error("解析AI建议JSON失败: {}", e.getMessage());
        }
        return result;
    }

    private String cleanJsonResponse(String response) {
        if (response == null || response.isBlank()) {
            return "[]";
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
