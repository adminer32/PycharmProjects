package com.saaes.system.client.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class DailyScoreAvgDTO {
    private LocalDate analysisDate;
    private Double avgOverallScore;
    private Double avgStabilityScore;
    private Double avgFluencyScore;
    private Double avgProficiencyScore;
}

