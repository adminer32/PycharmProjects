package com.system.service.ai.vo;

import lombok.Data;

@Data
public class AiPlanResult {
    private String goal;
    private Object weeklyPlans;
    private Object milestones;
    private String tips;
    private Long savedPlanId;
}
