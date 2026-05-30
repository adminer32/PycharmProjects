package com.saaes.system.client.dto;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.saaes.common.core.Constant;
import com.saaes.common.core.entity.BaseEntity;
import com.saaes.system.client.entity.AIAnalysisInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Transient;

import java.time.LocalDateTime;

@Schema(title = "消息卡片DTO")
@Table
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class AICardInfoDTO extends BaseEntity<Integer> {
    @Schema(title = "卡片id")
    private Integer id;

    @Schema(title = "卡片标题")
    private String title;

    @Schema(title = "视频Url")
    private String videoUrl;

    @Schema(title = "识别结果id")
    private Integer analysisId;

    @Schema(title = "识别结果信息")
    @TableField(exist = false)
    @JsonProperty("analysis")
    private AIAnalysisInfo analysisInfo;

    @Schema(title = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 获取完整的文件 URL
     */
    @Transient
    @Schema(title = "完整文件 URL")
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
