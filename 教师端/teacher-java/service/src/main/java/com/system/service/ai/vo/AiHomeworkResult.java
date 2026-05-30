package com.system.service.ai.vo;

import lombok.Data;

@Data
public class AiHomeworkResult {
    private String title;
    private String requirements;
    private Object tasks;
    private String evaluationCriteria;
    private Long savedHomeworkId;
}
