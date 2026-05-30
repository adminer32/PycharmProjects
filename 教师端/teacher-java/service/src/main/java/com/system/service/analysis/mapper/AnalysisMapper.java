package com.system.service.analysis.mapper;

import com.system.service.analysis.vo.AnalysisVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Mapper
public interface AnalysisMapper {


    BigDecimal getClassAvgStudyDuration(@Param("classId") Long classId);
    BigDecimal getClassHomeworkAvg(@Param("classId") Long classId);
    BigDecimal getClassAvgSkillPoints(@Param("classId") Long classId);
    
    List<Map<String, Object>> getStudentsByClass(@Param("classId") Long classId);


    List<AnalysisVO.TrendData> getClassDurationTrend(@Param("classId") Long classId);
    List<AnalysisVO.TrendData> getClassScoreTrend(@Param("classId") Long classId);


    List<AnalysisVO.SkillScore> getClassRadarData(@Param("classId") Long classId);


    List<Map<String, Object>> getClassCheckinCountsPerDay(@Param("classId") Long classId, @Param("yearMonth") String yearMonth);
    Integer getTotalClassStudents(@Param("classId") Long classId);
    Integer getClassCheckinCountInPeriod(@Param("classId") Long classId, @Param("startDate") String startDate, @Param("endDate") String endDate);

    Integer getUniqueCheckinStudentsInPeriod(@Param("classId") Long classId, @Param("startDate") String startDate, @Param("endDate") String endDate);

    List<Map<String, Object>> getClassHomeworkScoreDistribution(@Param("classId") Long classId);


    BigDecimal getStudentStudyDuration(@Param("studentId") Long studentId);
    BigDecimal getStudentHomeworkAvg(@Param("studentId") Long studentId);
    Integer getStudentHitSkillPoints(@Param("studentId") Long studentId);


    List<AnalysisVO.TrendData> getStudentDurationTrend(@Param("studentId") Long studentId);
    List<AnalysisVO.TrendData> getStudentScoreTrend(@Param("studentId") Long studentId);


    List<AnalysisVO.SkillScore> getStudentRadarData(@Param("studentId") Long studentId);
    List<AnalysisVO.ActionAbility> getStudentActionAnalysis(@Param("studentId") Long studentId);


    List<Integer> getStudentCheckindays(@Param("studentId") Long studentId, @Param("yearMonth") String yearMonth);
    Integer getStudentCheckinCountInPeriod(@Param("studentId") Long studentId, @Param("startDate") String startDate, @Param("endDate") String endDate);

    List<AnalysisVO.TrendData> getClassWeeklyLearningTrend(@Param("classId") Long classId);
    List<AnalysisVO.TrendData> getStudentWeeklyLearningTrend(@Param("studentId") Long studentId);
    Integer getQualifiedCheckinStudents(@Param("classId") Long classId, @Param("startDate") String startDate, @Param("endDate") String endDate, @Param("threshold") int threshold);

    Map<String, Object> getClassName(@Param("classId") Long classId);

    Map<String, Object> getTopStudyStudent(@Param("classId") Long classId, @Param("yearMonth") String yearMonth);
    Map<String, Object> getTopCheckinStudent(@Param("classId") Long classId, @Param("yearMonth") String yearMonth);
    Map<String, Object> getTopContinuousCheckinStudent(@Param("classId") Long classId, @Param("yearMonth") String yearMonth);

    List<Map<String, Object>> getStudentActionScores(@Param("studentId") Long studentId);

    Integer getClassTotalCheckinDays(@Param("classId") Long classId, @Param("yearMonth") String yearMonth);
}
