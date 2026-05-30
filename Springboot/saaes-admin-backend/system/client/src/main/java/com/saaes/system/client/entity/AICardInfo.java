package com.saaes.system.client.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.saaes.common.core.Constant;
import com.saaes.common.core.dao.AutoSetFun;
import com.saaes.common.core.entity.AutoSet;
import com.saaes.system.client.entity.AIAnalysisInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

import static com.saaes.common.core.Constant.BASE_URL;

@Schema(title = "消息卡片")
@TableName("ai_card_info")
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AICardInfo {

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
//    @Transient
//    @Schema(title = "完整文件 URL")
//    public String getVideoUrl() {
//        if (videoUrl == null || videoUrl.isEmpty()) {
//            return null;
//        }
//        if (videoUrl.startsWith("http://") || videoUrl.startsWith("https://")) {
//            return videoUrl;
//        }
//        return Constant.BASE_URL + videoUrl;  // 拼接完整的 URL
//    }

    /**
     * 获取相对的文件 URL
     */
//    @Transient
//    @Schema(title = "相对文件 URL")
//    public void setVideoUrl(String videoUrl) {
//        if (StringUtils.isNotBlank(videoUrl) && videoUrl.startsWith(BASE_URL)) {
//            this.videoUrl = videoUrl.substring(BASE_URL.length());
//        } else {
//            this.videoUrl = videoUrl;
//        }
//    }
}
