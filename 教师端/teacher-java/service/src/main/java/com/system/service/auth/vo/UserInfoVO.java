package com.system.service.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;


@Data
@Schema(description = "登录用户信息")
public class UserInfoVO implements Serializable {

    @Schema(title = "用户ID")
    private Long id;

    @Schema(title = "登录账号")
    private String username;

    @Schema(title = "显示名称")
    private String name;

    @Schema(title = "头像存储路径")
    private String avatar;

    @Schema(title = "用户角色")
    private String role;

    @Schema(title = "邮件地址")
    private String email;

    @Schema(title = "状态：0-未激活，1-激活")
    private Integer active;
}
