package com.system.service.learning.service;

import com.system.service.learning.vo.AiPlanSaveInput;
import com.system.service.learning.vo.LearningVO;
import java.util.List;
import java.util.Map;

public interface LearningService {

    LearningVO.Overview getOverview(String classId);

    List<LearningVO.StudentInfo> getStudentList(String classId, String keyword);

    LearningVO.PlanDetail getPlanByStudentId(Long studentId);

    LearningVO.PlanDetail getCurrentWeekPlan(Long studentId);

    LearningVO.PlanDetail getWeekPlanByNumber(Long studentId, Integer weekNumber);

    List<Map<String, Object>> getStudentAllPlans(Long studentId);

    List<Map<String, Object>> getStudentPlanGroups(Long studentId);

    boolean reviewPlan(LearningVO.ReviewInput input);

    void saveAiPlan(AiPlanSaveInput input);

    void deletePlanGroup(Long studentId, Long planGroupId);
}
