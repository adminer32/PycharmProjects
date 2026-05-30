package com.saaes.system.client.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.saaes.common.core.dao.AutoSetFun;
import com.saaes.common.core.entity.AutoSet;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.apache.ibatis.type.JdbcType;

import java.time.LocalDateTime;

@Schema(title = "AI聊天记录")
@TableName("ai_content_info")
@Data
public class AIContentInfo {

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(title = "聊天id")
    private Integer id;

    @Schema(title = "历史记录id")
    private Integer historyId;

    @Schema(title = "消息类型 card OR text")
    private String type;

    @Schema(title = "卡片id")
    private Integer cardId;

    @Schema(title = "卡片内容")
    @TableField(exist = false)
    private AICardInfo card;

    @Schema(title = "消息内容")
    private String text;

    @Schema(title = "发送者")
    private String sender;

    @Schema(title = "发生时间")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime sendTime;

    @Schema(title = "创建人")
    @TableField(fill = FieldFill.INSERT)
    private Integer createBy;

}
