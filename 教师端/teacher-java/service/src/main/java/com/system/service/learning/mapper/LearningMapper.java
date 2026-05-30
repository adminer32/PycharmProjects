package com.system.service.learning.mapper;

import com.system.service.learning.vo.LearningVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

@Mapper
public interface LearningMapper {

    Map<String, Object> getClassOverview(@Param("classId") String classId);

    List<LearningVO.NeedAttentionStudent> getNeedAttentionStudents(@Param("classId") String classId);

    List<LearningVO.StudentInfo> getStudentList(@Param("classId") String classId, @Param("keyword") String keyword);

    LearningVO.PlanDetail getPlanByStudentId(@Param("studentId") Long studentId);

    LearningVO.PlanDetail getCurrentWeekPlan(@Param("studentId") Long studentId);

    LearningVO.PlanDetail getWeekPlanByNumber(@Param("studentId") Long studentId, @Param("weekNumber") Integer weekNumber);

    List<Map<String, Object>> getStudentAllPlans(@Param("studentId") Long studentId);

    List<Map<String, Object>> getStudentPlanGroups(@Param("studentId") Long studentId);

    int updatePlanReview(@Param("planId") Long planId, @Param("status") String status,
                         @Param("reviewerId") Long reviewerId, @Param("comment") String comment);

    int batchInsertWeeklyPlans(@Param("list") List<Map<String, Object>> plans);

    int deletePlanGroup(@Param("studentId") Long studentId, @Param("planGroupId") Long planGroupId);

    int deleteStudentPlans(@Param("studentId") Long studentId);
}
