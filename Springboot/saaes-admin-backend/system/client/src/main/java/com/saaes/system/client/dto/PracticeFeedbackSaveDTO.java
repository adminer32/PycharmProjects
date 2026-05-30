package com.saaes.system.client.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(title = "练习评价反馈对象")
public class PracticeFeedbackSaveDTO {

    @Schema(title = "提交ID")
    @NotNull(message = "提交ID不能为空")
    private Integer submissionId;

    @Schema(title = "评价内容")
    @NotBlank(message = "评价不能为空")
    private String comment;
}
