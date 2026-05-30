package com.system.service.learning.vo;

import lombok.Data;
import java.util.List;

@Data
public class AiPlanWeekItem {
    private Long id;
    private Integer weekNumber;
    private String weekStart;
    private String weekEnd;
    private String theme;
    private List<String> focusSkills;
    private String weeklyGoal;
    private Object dailyTasks;
    private Integer progress;
    private String reviewStatus;
}
