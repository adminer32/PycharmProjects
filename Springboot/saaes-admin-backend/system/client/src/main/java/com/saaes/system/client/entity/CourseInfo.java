package com.saaes.system.client.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.saaes.common.core.Constant;
import com.saaes.common.core.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Data;
import lombok.EqualsAndHashCode;

import static com.saaes.common.core.Constant.BASE_URL;

@Schema(title = "课程信息")
@Table
@Data
@EqualsAndHashCode(callSuper = true)
public class CourseInfo extends BaseEntity<Integer> {

    @Schema(title = "课程名称")
    private String name;

    @Schema(title = "课程分类ID")
    private Integer categoryId;

    @Schema(title = "分类名称")
    @TableField(exist = false)
    @Transient
    private String categoryName;

    @Schema(title = "课程详情")
    private String content;

    @Schema(title = "课程描述")
    private String description;

    @Schema(title = "视频ID")
    private Integer videoUrlId;

    @TableField(exist = false)
    @Transient
    @Schema(title = "视频url")
    private String videoUrl;

    @TableField(exist = false)
    @Transient
    @Schema(title = "视频时长")
    private String videoDuration;

    @TableField(exist = false)
    @Transient
    @Schema(title = "等级名称")
    private String levelName;

    @Schema(title = "等级id")
    private Integer levelId;

    @Schema(title = "笔记id")
    private Integer aiNoteId;

    @Schema(title = "任课教师名称")
    private String teacherName;

    @Schema(title = "课程缩略图")
    private String coverImageUrl;

    @Schema(title = "是否启用")
    private Boolean enabled;

    /**
     * 获取完整缩略图的文件 URL
     */
    @Transient
    @Schema(title = "完整缩略图文件 URL")
    public String getCoverImageUrl() {
        if (coverImageUrl == null || coverImageUrl.isEmpty()) {
            return BASE_URL + "/files/2025/038856461993_1038984831.jpg";
        }
        if (coverImageUrl.startsWith("http://") || coverImageUrl.startsWith("https://")) {
            return coverImageUrl;
        }
        return Constant.BASE_URL + coverImageUrl;  // 拼接完整的 URL
    }

    @Transient
    @Schema(title = "缩略图文件 URL")
    public String setCoverImageUrl() {
        if (StringUtils.isNotBlank(coverImageUrl) && coverImageUrl.startsWith(BASE_URL)) {
            this.coverImageUrl = coverImageUrl.substring(BASE_URL.length());
        }
        return this.coverImageUrl;
    }

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
