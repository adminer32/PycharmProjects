package com.system.service.homework.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(title = "发布作业请求")
@Data
public class CreateHomeworkDTO implements Serializable {

    @Schema(title = "作业标题", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Schema(title = "作业要求", requiredMode = Schema.RequiredMode.REQUIRED)
    private String requirements;

    @Schema(title = "示范视频URL")
    private String demoVideoUrl;

    @Schema(title = "截止时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime deadline;

    @Schema(title = "班级ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long classId;
}
