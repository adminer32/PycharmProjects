package com.saaes.system.admin.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.saaes.common.core.validator.CreateGroup;
import com.saaes.common.core.validator.UpdateGroup;
import lombok.Data;

@Data
@Schema(title = "管理员用户保存对象")
public class AdminUserSaveDTO {

    @Schema(title = "用户ID (更新时必传)")
    @NotNull(message = "用户ID不能为空", groups = UpdateGroup.class)
    private Integer id;

    @Schema(title = "用户名")
    @NotBlank(message = "用户名不能为空")
    private String name;

    @Schema(title = "账号")
    @NotBlank(message = "账号不能为空", groups = CreateGroup.class)
    private String code;

    @Schema(title = "密码 (新增时必传, 更新时不传则不修改)")
    @NotBlank(message = "密码不能为空", groups = CreateGroup.class)
    private String password;

    @Schema(title = "手机号")
    private String telephone;

    @Schema(title = "邮箱")
    private String email;

    @Schema(title = "是否启用")
    private Boolean enabled;

    @Schema(title = "用户状态 (1: 正常, 2: 锁定)")
    private Integer status;

    @Schema(title = "头像地址")
    private String avatar;

    @Schema(title = "平台标识 (Web/xcx)")
    private String platform;

    @Schema(title = "角色ID")
    private Integer roleId;
}
