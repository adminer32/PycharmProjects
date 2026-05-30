package com.system.service.learning.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.system.service.learning.mapper.LearningMapper;
import com.system.service.learning.service.LearningService;
import com.system.service.learning.vo.AiPlanSaveInput;
import com.system.service.learning.vo.LearningVO;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cn.dev33.satoken.SaManager.log;

@Service
public class LearningServiceImpl implements LearningService {

    @Resource
    private LearningMapper learningMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public LearningVO.Overview getOverview(String classId) {
        Map<String, Object> raw = learningMapper.getClassOverview(classId);
        List<LearningVO.NeedAttentionStudent> needAttentionList = learningMapper.getNeedAttentionStudents(classId);

        LearningVO.Overview overview = new LearningVO.Overview();
        if (raw != null && !raw.isEmpty()) {
            Number totalStudents = (Number) raw.get("totalStudents");
            Number pendingReviewCount = (Number) raw.get("pendingReviewCount");
            Number completionRate = (Number) raw.get("completionRate");
            Number needAttentionCount = (Number) raw.get("needAttentionCount");
            overview.setTotalStudents(totalStudents != null ? totalStudents.intValue() : 0);
            overview.setPendingReviewCount(pendingReviewCount != null ? pendingReviewCount.intValue() : 0);
            overview.setCompletionRate(completionRate != null ? completionRate.intValue() : 0);
            overview.setNeedAttentionCount(needAttentionCount != null ? needAttentionCount.intValue() : 0);
        } else {
            overview.setTotalStudents(0);
            overview.setPendingReviewCount(0);
            overview.setCompletionRate(0);
            overview.setNeedAttentionCount(0);
        }

        overview.setNeedAttentionStudents(needAttentionList != null ? needAttentionList : new ArrayList<>());
        return overview;
    }

    @Override
    public List<LearningVO.StudentInfo> getStudentList(String classId, String keyword) {
        List<LearningVO.StudentInfo> list = learningMapper.getStudentList(classId, keyword);
        if (list == null) return new ArrayList<>();
        for (LearningVO.StudentInfo info : list) {
            if (info.getWeakness() == null || info.getWeakness().isEmpty()) {
                info.setWeakness("");
            }
        }
        return list;
    }

    @Override
    public LearningVO.PlanDetail getPlanByStudentId(Long studentId) {
        LearningVO.PlanDetail detail = learningMapper.getPlanByStudentId(studentId);
        if (detail == null) return null;
        return parsePlanTasks(detail);
    }

    @Override
    public boolean reviewPlan(LearningVO.ReviewInput input) {
        String status = "approve".equals(input.getAction()) ? "批准" : "驳回";
        return learningMapper.updatePlanReview(input.getPlanId(), status, input.getTeacherId(), input.getComment()) > 0;
    }

    @Override
    public LearningVO.PlanDetail getCurrentWeekPlan(Long studentId) {
        LearningVO.PlanDetail detail = learningMapper.getCurrentWeekPlan(studentId);
        if (detail == null) return null;
        return parsePlanTasks(detail);
    }

    @Override
    public LearningVO.PlanDetail getWeekPlanByNumber(Long studentId, Integer weekNumber) {
        log.info("getWeekPlanByNumber - studentId: {}, weekNumber: {}", studentId, weekNumber);
        LearningVO.PlanDetail detail = learningMapper.getWeekPlanByNumber(studentId, weekNumber);
        log.info("getWeekPlanByNumber - detail: {}", detail);
        if (detail == null) {
            log.warn("getWeekPlanByNumber - No plan found for studentId: {}, weekNumber: {}", studentId, weekNumber);
            return null;
        }
        log.info("getWeekPlanByNumber - tasks before parse: {}", detail.getTasks());
        return parsePlanTasks(detail);
    }

    @Override
    public List<Map<String, Object>> getStudentAllPlans(Long studentId) {
        return learningMapper.getStudentAllPlans(studentId);
    }

    @Override
    public List<Map<String, Object>> getStudentPlanGroups(Long studentId) {
        return learningMapper.getStudentPlanGroups(studentId);
    }

    @Override
    public void saveAiPlan(AiPlanSaveInput input) {
        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(DayOfWeek.MONDAY);
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        List<Map<String, Object>> plans = new ArrayList<>();
        List<Map> weeklyPlansList = new ArrayList<>();

        if (input.getWeeklyPlans() instanceof List) {
            weeklyPlansList = (List<Map>) input.getWeeklyPlans();
        }

        learningMapper.deleteStudentPlans(input.getStudentId());

        for (int i = 0; i < input.getTotalWeeks(); i++) {
            LocalDate weekStart = monday.plusWeeks(i);
            LocalDate weekEnd = weekStart.plusDays(6);

            Map<String, Object> weekPlan = new HashMap<>();
            weekPlan.put("studentId", input.getStudentId());
            weekPlan.put("classId", input.getClassId());
            weekPlan.put("goal", input.getGoal());

            String tasksJson = "[]";
            Map<String, Object> fullJson = new HashMap<>();
            fullJson.put("goal", input.getGoal());
            fullJson.put("milestones", input.getMilestones());
            fullJson.put("tips", input.getTips());

            if (i < weeklyPlansList.size()) {
                Map weekData = weeklyPlansList.get(i);
                try {
                    Object dailyTasksObj = weekData.get("dailyTasks");
                    if (dailyTasksObj != null) {
                        tasksJson = objectMapper.writeValueAsString(dailyTasksObj);
                    } else {
                        List<Map<String, Object>> fallbackTasks = new ArrayList<>();
                        Object focusSkills = weekData.get("focusSkills");
                        String theme = weekData.get("theme") != null ? weekData.get("theme").toString() : "训练";
                        if (focusSkills instanceof List) {
                            for (Object skill : (List<?>) focusSkills) {
                                Map<String, Object> task = new HashMap<>();
                                task.put("name", skill.toString());
                                task.put("content", "进行" + skill.toString() + "专项训练（" + theme + "）");
                                task.put("duration", 30);
                                fallbackTasks.add(task);
                            }
                        }
                        if (!fallbackTasks.isEmpty()) {
                            tasksJson = objectMapper.writeValueAsString(fallbackTasks);
                        }
                    }
                    fullJson.put("weeklyPlan", weekData);
                } catch (Exception e) {
                    tasksJson = "[]";
                }
            }

            weekPlan.put("tasks", tasksJson);
            weekPlan.put("weekStartDate", weekStart.format(dateFormatter));
            weekPlan.put("weekEndDate", weekEnd.format(dateFormatter));
            weekPlan.put("weekNumber", i + 1);
            weekPlan.put("totalWeeks", input.getTotalWeeks());

            try {
                weekPlan.put("fullPlanJson", objectMapper.writeValueAsString(fullJson));
            } catch (Exception e) {
                weekPlan.put("fullPlanJson", "{}");
            }

            plans.add(weekPlan);
        }

        if (!plans.isEmpty()) {
            learningMapper.batchInsertWeeklyPlans(plans);
        }
    }

    @Override
    public void deletePlanGroup(Long studentId, Long planGroupId) {
        learningMapper.deletePlanGroup(studentId, planGroupId);
    }

    private LearningVO.PlanDetail parsePlanTasks(LearningVO.PlanDetail detail) {
        try {
            String tasksJson = detail.getTasksJson();
            log.info("parsePlanTasks - tasksJson: {}", tasksJson);
            
            if (tasksJson == null || tasksJson.isEmpty() || tasksJson.equals("[]") || tasksJson.equals("null")) {
                detail.setTasks(new ArrayList<>());
                return detail;
            }
            
            List<LearningVO.TrainingTask> taskList = objectMapper.readValue(tasksJson, new TypeReference<List<LearningVO.TrainingTask>>() {});
            detail.setTasks(taskList);
            log.info("parsePlanTasks - Parsed {} tasks", taskList.size());
        } catch (Exception e) {
            log.error("parsePlanTasks - Error parsing tasks", e);
            detail.setTasks(new ArrayList<>());
        }
        return detail;
    }
}
