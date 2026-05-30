package com.saaes.system.client.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.saaes.common.core.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;


@Schema(title = "练习任务")
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("practice_task")
public class PracticeTask extends BaseEntity<Integer> {

    @Schema(title = "练习任务标题")
    private String title;

    @Schema(title = "练习任务要求")
    private String content;

    @Schema(title = "发布老师id")
    @TableField("teacher_id")
    private Integer teacherId;

    @Schema(title = "老师对应的班级id")
    @TableField("class_id")
    private Integer classId;

    @Schema(title = "截止时间")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDateTime deadline;

}
