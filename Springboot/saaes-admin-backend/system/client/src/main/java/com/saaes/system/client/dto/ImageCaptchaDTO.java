package com.saaes.system.client.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 验证码图片DTO
 */
@Schema(title = "存放图形验证码数据")
@Data
public class ImageCaptchaDTO implements Serializable {

    @Schema(title = "key")
    private String captchaKey;
    @Schema(title = "base64图形数据")
    private String imageBase64;
    @Schema(title = "验证码")
    private String captchaCode;
}
