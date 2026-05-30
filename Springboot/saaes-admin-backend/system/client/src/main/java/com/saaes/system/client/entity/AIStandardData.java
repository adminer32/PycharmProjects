package com.saaes.system.client.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(title = "标准动作库")
@TableName("ai_standard_data")
@Data
public class AIStandardData {

    @Schema(title = "标准动作数据id")
    private Integer id;

    @Schema(title = "标准动作名称")
    private String name;

    @Schema(title = "左脚标准X轴数据")
    private List<Integer> standardLeftXData;

    @Schema(title = "左脚标准Y轴数据")
    private List<Integer> standardLeftYData;

    @Schema(title = "右脚标准X轴数据")
    private List<Integer> standardRightXData;

    @Schema(title = "右脚标准Y轴数据")
    private List<Integer> standardRightYData;

}
