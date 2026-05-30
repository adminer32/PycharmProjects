package com.saaes.system.client.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.saaes.common.core.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Entity
@Schema(title = "AI视频分析历史记录信息")
@TableName("ai_video_history")
@Table(name = "ai_video_history")
@Data
@EqualsAndHashCode(callSuper = true)
public class AIVideosHistory extends BaseEntity<Integer> {

    @Schema(title = "历史记录标题")
    private String title;

    @Schema(title = "历史记录聊天对话")
    @TableField(exist = false)
    @Transient
    private List<AIContentInfo> contentList;

}
