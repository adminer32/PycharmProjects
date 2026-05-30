package com.system.service.homework.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Schema(title = "批改作业请求")
@Data
public class GradeSubmissionDTO implements Serializable {

    @Schema(title = "教师评分", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal teacherScore;

    @Schema(title = "教师评语")
    private String teacherComment;

    @Schema(title = "AI 建议")
    private String aiSuggestion;
}
