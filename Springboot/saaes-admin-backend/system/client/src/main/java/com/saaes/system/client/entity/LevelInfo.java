package com.saaes.system.client.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("level_info")
public class LevelInfo {

    @TableId(value = "level_id", type = IdType.AUTO)
    private Integer levelId;
    private String levelName;
}
