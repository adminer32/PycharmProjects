package com.saaes.system.client.entity;

import lombok.Data;

@Data
public class AnalysisResult {
    private Double stability_score;
    private Double proficiency_score;
    private Double fluency_score;
    private Double overall_score;
    private String stability_improvement;
    private String proficiency_enhancement;
    private String fluency_promotion;
    private String general_advice;
}

