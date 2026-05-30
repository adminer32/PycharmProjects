package com.system.service.analysis.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class AnalysisVO {

    @Data
    public static class ClassDashboard {
        private ClassMetrics metrics;
        private List<TrendData> durationTrend;
        private List<TrendData> scoreTrend;
        private List<SkillScore> radarData;
        private CheckinSummary checkinData;
        private Map<String, Integer> scoreDistribution;
        private List<ActionAbility> actionDistribution;
    }

    @Data
    public static class StudentDashboard {
        private StudentMetrics metrics;
        private List<TrendData> durationTrend;
        private List<TrendData> scoreTrend;
        private List<SkillScore> radarData;
        private List<ActionAbility> actionAnalysis;
        private List<Integer> personalCheckinData;
        private StudentTags tags;
    }

    @Data
    public static class StudentTags {
        private String progressTag;
        private String weakTag;
        private String strongTag;
        private BigDecimal progressPercent;
    }

    @Data
    public static class ClassMetrics {
        private BigDecimal avgStudyDuration;
        private BigDecimal homeworkAvg;
        private BigDecimal classCheckinRate;
        private BigDecimal avgSkillPoints;
    }

    @Data
    public static class StudentMetrics {
        private BigDecimal studyDuration;
        private BigDecimal homeworkAvg;
        private BigDecimal checkinRate;
        private Integer skillPoints;
        private Integer monthTotalCheckin; // 本月累计打卡天数
        private Integer continuousCheckin; // 连续打卡天数
    }

    @Data
    public static class TrendData {
        private String label;
        private BigDecimal value;
    }

    @Data
    public static class SkillScore {
        private String skillType;
        private BigDecimal personalScore;
        private BigDecimal classAvgScore;
    }

    @Data
    public static class ActionAbility {
        private String name;
        private BigDecimal avg;
        private BigDecimal historyAvg;
        private String feedback;
        private String recentScores;
    }

    @Data
    public static class CheckinSummary {
        private BigDecimal weekRate;
        private BigDecimal monthRate;
        private Map<Integer, Integer> dayCounts;
        private Integer qualifiedCount;
        private String topContinuousStudent;
        private Integer topContinuousDays;
        private String topStudyStudent;
        private BigDecimal topStudyHours;
        private String topCheckinStudent;
        private Integer topCheckinDays;
    }
}
