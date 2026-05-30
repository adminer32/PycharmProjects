package com.system.service.learning.vo;

import lombok.Data;

@Data
public class AiPlanSaveInput {
    private Long studentId;
    private Long classId;
    private Integer totalWeeks;
    private String goal;
    private Object weeklyPlans;
    private Object milestones;
    private String tips;
}
