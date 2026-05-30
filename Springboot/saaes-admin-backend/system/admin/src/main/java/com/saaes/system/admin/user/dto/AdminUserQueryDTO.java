package com.saaes.system.admin.user.dto;

import com.saaes.common.core.admin.BasePageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Schema(title = "管理员用户查询参数")
@EqualsAndHashCode(callSuper = true)
public class AdminUserQueryDTO extends BasePageQuery {

    @Schema(title = "用户姓名")
    private String name;

    @Schema(title = "用户代码/账号")
    private String code;

    @Schema(title = "邮箱")
    private String email;

    @Schema(title = "手机号")
    private String telephone;

    @Schema(title = "启用状态")
    private Boolean enabled;
}
