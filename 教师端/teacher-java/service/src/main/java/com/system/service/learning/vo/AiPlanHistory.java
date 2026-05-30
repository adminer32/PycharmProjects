package com.system.service.learning.vo;

import lombok.Data;
import java.util.List;

@Data
public class AiPlanHistory {
    private Long planGroupId;
    private Long studentId;
    private String studentName;
    private Integer totalWeeks;
    private Integer completedWeeks;
    private String overallGoal;
    private String createdAt;
    private List<AiPlanWeekItem> weeks;
}
