package com.saaes.system.client.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(title = "练习提交对象")
public class PracticeSubmissionDTO {

    @Schema(title = "任务ID")
    @NotNull(message = "任务ID不能为空")
    private Integer taskId;

    @Schema(title = "文件安全码")
    @NotNull(message = "文件安全码不能为空")
    private String fileCode;
}
