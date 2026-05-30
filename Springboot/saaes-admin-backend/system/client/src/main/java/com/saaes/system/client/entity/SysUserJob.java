package com.saaes.system.client.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.saaes.common.core.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(title = "系统用户岗位表")
@TableName("sys_user_job")
@Table
@Data
@EqualsAndHashCode(callSuper = true)
public class SysUserJob extends BaseEntity<Integer> {

    @Schema(title = "数据类型", allowableValues = { "1", "2" }, description = "1：用户，2：用户组")
    private Integer type;
    @Schema(title = "用户id或者用户组的id")
    @TableField("user_id")
    private Integer userId;
    @Schema(title = "机构id")
    @TableField("sys_org_id")
    private Integer sysOrgId;
    @Schema(title = "角色id")
    @TableField("sys_role_id")
    private Integer sysRoleId;
    @Schema(title = "是否启用1：是，0：否")
    private Boolean enabled;

    @Schema(title = "机构代码")
    @Transient
    @TableField(exist = false)
    private String orgCode;
    @Schema(title = "机构名称")
    @Transient
    @TableField(exist = false)
    private String orgName;
    @Schema(title = "角色名称")
    @Transient
    @TableField(exist = false)
    private String roleName;
}
