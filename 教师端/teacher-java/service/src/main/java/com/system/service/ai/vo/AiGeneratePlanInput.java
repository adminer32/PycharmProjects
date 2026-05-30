package com.system.service.ai.vo;

import lombok.Data;

@Data
public class AiGeneratePlanInput {
    private Long studentId;
    private Integer planDuration;
    private String goal;
}
