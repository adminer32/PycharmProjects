package com.saaes.system.client.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.saaes.common.core.dao.AutoSetFun;
import com.saaes.common.core.entity.AutoSet;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.apache.ibatis.annotations.Options;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@TableName("schedule_event")
public class ScheduleEvent {

    @Schema(title = "事件ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(title = "事件时间,年月日")
    private LocalDate scheduleDate;

    @Schema(title = "事件时间,时分秒")
    private LocalTime time;

    @Schema(title = "事件类型")
    private String type;

    @Schema(title = "事件内容")
    private String content;

    @Schema(title = "发生时间")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(title = "创建人")
    @TableField(fill = FieldFill.INSERT)
    private Integer createBy;

    @Schema(title = "是否已删除")
    private Boolean deleted;
}


