package com.system.service.learning.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import java.util.List;

@Data
public class LearningVO {

    @Data
    public static class Overview {
        private Integer completionRate;
        private Integer pendingReviewCount;
        private Integer totalStudents;
        private Integer needAttentionCount;
        private List<NeedAttentionStudent> needAttentionStudents;
    }

    @Data
    public static class NeedAttentionStudent {
        private Long id;
        private String name;
        private String reason;
    }

    @Data
    public static class StudentInfo {
        private Long id;
        private String name;
        private String studentId;
        private String planStatus;
        private String weakness;
        private Integer progress;
    }

    @Data
    public static class PlanDetail {
        private Long id;
        private String goal;
        @JsonIgnore
        private String tasksJson;
        private Integer progress;
        private String reviewStatus;
        private String reviewComment;
        private String generatedAt;
        private Integer weekNumber;
        private Integer totalWeeks;
        private String weekStart;
        private String weekEnd;
        private Object fullPlanJson;
        private List<TrainingTask> tasks;
    }

    @Data
    public static class TrainingTask {
        private String name;
        private String spec;
        private String video;
        private String action;
        private Integer target;
        private String content;
        private Integer duration;
        private String day;
        
        public String getDisplayName() {
            if (name != null && !name.isEmpty()) return name;
            if (action != null && !action.isEmpty()) return action;
            return "训练任务";
        }
        
        public String getDisplaySpec() {
            if (spec != null && !spec.isEmpty()) return spec;
            if (target != null) return "目标: " + target;
            if (content != null && !content.isEmpty()) return content;
            return "";
        }
    }

    @Data
    public static class ReviewInput {
        private Long planId;
        private Long teacherId;
        private String action;
        private String comment;
    }
}
