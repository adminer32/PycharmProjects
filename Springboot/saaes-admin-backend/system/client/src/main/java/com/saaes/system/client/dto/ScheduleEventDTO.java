package com.saaes.system.client.dto;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class ScheduleEventDTO {

    @Schema(title = "事件时间,年月日")
    private LocalDate scheduleDate;

    @Schema(title = "事件时间,时分秒")
    private LocalTime time;

    @Schema(title = "事件类型")
    private String type;

    @Schema(title = "事件内容")
    private String content;

}


