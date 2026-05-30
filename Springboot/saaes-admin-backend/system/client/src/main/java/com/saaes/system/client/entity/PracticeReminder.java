package com.saaes.system.client.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.saaes.common.core.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;


@Schema(title = "练习提醒")
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("practice_reminder")
public class PracticeReminder extends BaseEntity<Integer> {

    @Schema(title = "练习任务id")
    @TableField("task_id")
    private Integer taskId;

    @Schema(title = "学生id")
    @TableField("student_id")
    private Integer studentId;

    @Schema(title = "提醒内容")
    private String content;

    @Schema(title = "是否已读 (0: 未读, 1: 已读)")
    @TableField("is_read")
    private Integer isRead;

}
