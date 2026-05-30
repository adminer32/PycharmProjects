package com.saaes.system.client.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.saaes.common.core.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Schema(title = "AI视频分析结果")
@TableName("ai_analysis_result")
@Data
@EqualsAndHashCode(callSuper = true)
public class AIAnalysisInfo extends BaseEntity<Integer> {

    @Schema(title = "总体性评分")
    private Double overallScore;

    @Schema(title = "稳定性评分")
    private Double stabilityScore;

    @Schema(title = "流畅性评分")
    private Double fluencyScore;

    @Schema(title = "熟练度评分")
    private Double proficiencyScore;

    @Schema(title = "稳定性改进")
    private String stabilityImprovement;

    @Schema(title = "熟练度改进")
    private String proficiencyEnhancement;

    @Schema(title = "流利度改进")
    private String fluencyPromotion;

    @Schema(title = "一般建议")
    private String generalAdvice;

    @Schema(title = "用户左脚X轴数据")
//    @TableField(typeHandler = Fastjson2TypeHandler.class)
    private List<Integer> selfLeftXData;

    @Schema(title = "用户左脚Y轴数据")
//    @TableField(typeHandler = Fastjson2TypeHandler.class)
    private List<Integer> selfLeftYData;

    @Schema(title = "用户右脚X轴数据")
//    @TableField(typeHandler = Fastjson2TypeHandler.class)
    private List<Integer> selfRightXData;

    @Schema(title = "用户右脚Y轴数据")
//    @TableField(typeHandler = Fastjson2TypeHandler.class)
    private List<Integer> selfRightYData;

    @Schema(title = "动作名称")
    private String motionName;

    @Schema(title = "视频文件Url")
    private String videoUrl;

    @Schema(title = "任务id")
    private String taskId;

    @Schema(title = "任务状态")
    private String taskStatus;


}
