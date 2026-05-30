package com.saaes.system.client.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(title = "文件上传响应对象")
public class FileUploadResponseDTO {

    @Schema(title = "文件名")
    private String name;

    @Schema(title = "文件安全码")
    private String fileCode;

    @Schema(title = "文件访问URL")
    private String url;

    @Schema(title = "文件大小")
    private Long size;

    @Schema(title = "文件类型")
    private String contentType;

    @Schema(title = "文件后缀")
    private String suffix;

}
