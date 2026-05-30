package com.saaes.system.admin.auth.vo;

import com.saaes.system.admin.user.vo.AdminUserVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Schema(title = "Token校验结果VO")
@Data
@Builder
public class AdminTokenCheckVO implements Serializable {

    @Schema(title = "Token是否有效")
    private Boolean valid;

    @Schema(title = "用户信息")
    private AdminUserVO user;

    @Schema(title = "角色标识列表")
    private List<String> roles;

    @Schema(title = "权限码列表")
    private List<String> permissions;
}
