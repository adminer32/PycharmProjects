package com.saaes.system.client.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(title = "用户注册信息")
@Data
public class SysRegisterRequestDTO {

    @Schema(title = "用户名")
    private String username;

    @Schema(title = "昵称")
    private String nickname;

    @Schema(title = "密码")
    private String password;

    @Schema(title = "重复密码")
    private String rePassword;

    @Schema(title = "头像url")
    private String avatar;

    @Schema(title = "手机号码")
    private String telephone;

    @Schema(title = "用户语言区域")
    private String locale;

    @Schema(title = "角色ID")
    private Integer sysRoleId;

    @Schema(title = "注册平台，Web or App")
    private String platform;

    @Schema(title = "uniApp openId")
    private String openid;

}
