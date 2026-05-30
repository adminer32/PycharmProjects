package com.system.service.ai.vo;

import lombok.Data;

@Data
public class AiGenerateHomeworkInput {
    private String subject;
    private String difficulty;
    private Integer studentCount;
    private String deadline;
    private Long classId;
}
