package com.system.service.analysis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("motion_analysis_data")
public class MotionAnalysisData {

    @TableId(type = IdType.AUTO)
    private Integer id;

    @TableField("student_id")
    private Integer studentId;

    @TableField("analysis_data")
    private String analysisData;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
