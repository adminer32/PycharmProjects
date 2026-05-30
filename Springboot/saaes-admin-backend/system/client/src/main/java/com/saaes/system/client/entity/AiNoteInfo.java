package com.saaes.system.client.entity;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.saaes.common.core.Constant;
import com.saaes.common.core.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Data;
import lombok.EqualsAndHashCode;


@Schema(title = "AI视频笔记")
@Table
@Data
@EqualsAndHashCode(callSuper = true)
public class AiNoteInfo extends BaseEntity<Integer> {

    @Schema(title = "关联course_id")
    private Integer courseId;

    @Schema(title = "关联sys_file文件id")
    private Integer videoId;

    @Schema(title = "任务id")
    private String taskId;

    @Schema(title = "视频链接")
    private String videoUrl;

    @Schema(title = "视频时长")
    @Transient
    private String video_duration;

    @Schema(title = "任务标题")
    private String title;

    @Schema(title = "章节速览结果")
    @JsonRawValue
    private String autoChapters;

    @Schema(title = "大模型摘要结果")
    @JsonRawValue
    private String summarization;

    @Schema(title = "智能纪要结果")
    @JsonRawValue
    private String meetingAssistance;

    @Schema(title = "个人笔记")
    private String personalNote;

    @Schema(title = "任务状态")
    private String taskStatus;

    /**
     * 获取完整的视频文件 URL
     */
    @Transient
    @Schema(title = "完整视频文件 URL")
    public String getVideoUrl() {
        if (videoUrl == null || videoUrl.isEmpty()) {
            return null;
        }
        if (videoUrl.startsWith("http://") || videoUrl.startsWith("https://")) {
            return videoUrl;
        }
        return Constant.BASE_URL + videoUrl;  // 拼接完整的 URL
    }

}
