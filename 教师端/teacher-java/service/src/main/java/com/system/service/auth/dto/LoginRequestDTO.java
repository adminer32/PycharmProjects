package com.system.service.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(title = "登录请求体")
@Data
public class LoginRequestDTO {

    @Schema(title = "用户名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @Schema(title = "密码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @Schema(title = "Cloudflare Turnstile 安全校验 Token")
    private String turnstileToken;
}
