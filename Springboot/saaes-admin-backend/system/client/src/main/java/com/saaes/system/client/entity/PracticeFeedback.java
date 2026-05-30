package com.saaes.system.client.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.saaes.common.core.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;


@Schema(title = "练习评价")
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("practice_feedback")
public class PracticeFeedback extends BaseEntity<Integer> {

    @Schema(title = "学生提交id")
    @TableField("submission_id")
    private Integer submissionId;

    @Schema(title = "评价老师id")
    @TableField("teacher_id")
    private Integer teacherId;

    @Schema(title = "老师评语")
    private String comment;

}
