package com.system.service.analysis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("student_physique")
public class StudentPhysique {

    @TableId(type = IdType.AUTO)
    private Integer id;

    @TableField("student_id")
    private Integer studentId;

    private BigDecimal weight;
    private BigDecimal bmi;
    private BigDecimal fatPercentage;
    private BigDecimal skeletalMuscleMass;
    private BigDecimal visceralFatLevel;
    private BigDecimal limbSkeletalMuscleIndex;
    private BigDecimal estimatedWaistHipRatio;
    private String bodyType;
    private String bodyShape;
    private Integer basalMetabolismRate;
    private BigDecimal moistureRate;
    private BigDecimal boneSaltAmount;
    private BigDecimal proteinPercentage;
    private BigDecimal leanBodyMass;
    private Integer bodyAge;
    private Integer heartRate;
    private BigDecimal segmentMoisture;
    private BigDecimal segmentProtein;
    private BigDecimal segmentFatMass;
    private BigDecimal segmentBoneSalt;
    private BigDecimal segmentFatTotal;
    private BigDecimal segmentFatRightUpper;
    private BigDecimal segmentFatLeftUpper;
    private BigDecimal segmentFatTrunk;
    private BigDecimal segmentFatRightLower;
    private BigDecimal segmentFatLeftLower;
    private BigDecimal segmentSkeletalMuscleTotal;
    private BigDecimal segmentSkeletalRightUpper;
    private BigDecimal segmentSkeletalLeftUpper;
    private BigDecimal segmentSkeletalTrunk;
    private BigDecimal segmentSkeletalRightLower;
    private BigDecimal segmentSkeletalLeftLower;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
