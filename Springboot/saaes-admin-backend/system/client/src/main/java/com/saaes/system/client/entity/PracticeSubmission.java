package com.saaes.system.client.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.saaes.common.core.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 练习提交表
 */
@Schema(title = "练习提交")
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("practice_submission")
public class PracticeSubmission extends BaseEntity<Integer> {

    @Schema(title = "练习任务id")
    @TableField("task_id")
    private Integer taskId;

    @Schema(title = "学生id")
    @TableField("student_id")
    private Integer studentId;

    @Schema(title = "视频url")
    @TableField("video_url")
    private String videoUrl;

    @Schema(title = "关联视频url的id")
    @TableField("file_id")
    @JsonIgnore
    private Integer fileId;

    @Schema(title = "批阅状态 (0: 未批阅, 1: 已批阅)")
    private Integer status;

    @Schema(title = "文件安全码")
    @TableField("file_code")
    private String fileCode;

    /**
     * 获取安全的视频URL
     * 使用fileCode拼接URL，保护学生提交的练习视频不被遍历
     */
    public String getSecureVideoUrl() {
        if (fileCode != null && !fileCode.isEmpty()) {
            return "/api/file/operation/preview/" + fileCode;
        }
        return videoUrl;
    }

}
