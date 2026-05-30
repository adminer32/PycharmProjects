package com.saaes.system.admin.user.vo;

import com.saaes.common.core.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(title = "管理员用户信息VO")
public class AdminUserVO extends BaseEntity<Integer> {

    @Schema(title = "用户代码")
    private String code;

    @Schema(title = "用户名")
    private String name;

    @Schema(title = "头像")
    private String avatar;

    @Schema(title = "手机号码")
    private String telephone;

    @Schema(title = "邮箱")
    private String email;

    @Schema(title = "状态（1：正常，2：锁定）")
    private Integer status;

    @Schema(title = "是否启用")
    private Boolean enabled;

    @Schema(title = "角色ID")
    private Integer roleId;

    @Schema(title = "角色名称")
    private String roleName;
}
