package com.system.service.homework.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(title = "作业列表视图")
@Data
public class HomeworkListVO implements Serializable {

    @Schema(title = "作业ID")
    private Long id;

    @Schema(title = "作业标题")
    private String title;

    @Schema(title = "作业要求")
    private String requirements;

    @Schema(title = "截止时间")
    private LocalDateTime deadline;

    @Schema(title = "状态")
    private Integer status;

    @Schema(title = "已提交人数")
    private Integer submissionCount;

    @Schema(title = "总人数")
    private Integer totalCount;

    @Schema(title = "待批改数")
    private Integer pendingGradeCount;

    @Schema(title = "平均分")
    private BigDecimal averageScore;

    @Schema(title = "创建时间")
    private LocalDateTime createdAt;
}
