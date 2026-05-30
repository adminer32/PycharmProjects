package com.saaes.system.client.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(title = "用户登录信息")
@Data
public class SysLoginRequestDTO {

    @Schema(title = "用户名")
    private String username;

    @Schema(title = "密码")
    private String password;

    @Schema(title = "UUID")
    private String captchaKey;

    @Schema(title = "验证码")
    private String captchaCode;

    @Schema(title = "用户的语言区域")
    private String locale;

    @Schema(title = "语言区域的显示名称")
    private String localeLabel;

    @Schema(title = "是否演示账号")
    private Boolean isDemo;
}
