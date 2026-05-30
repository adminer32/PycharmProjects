package com.system.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.system.dao.AutoSetFun;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@MappedSuperclass
public class BaseEntity<I extends Serializable> implements Serializable {

    @Schema(title = "主键ID")
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    protected I id;

    @Schema(title = "创建时间")
    @AutoSet(AutoSetFun.INSERT_NOW)
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(title = "修改时间")
    @AutoSet(AutoSetFun.UPDATE_NOW)
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime updateTime;

    @Schema(title = "创建人")
    @AutoSet(AutoSetFun.INSERT_BY)
    @TableField(fill = FieldFill.INSERT)
    private Integer createBy;

    @Schema(title = "修改人")
    @AutoSet(AutoSetFun.UPDATE_BY)
    @TableField(fill = FieldFill.INSERT)
    private Integer updateBy;

    @Schema(title = "是否已删除")
    @AutoSet(AutoSetFun.DEFAULT_FALSE)
    @TableLogic(value = "false", delval = "true")
    private Boolean deleted;
}
