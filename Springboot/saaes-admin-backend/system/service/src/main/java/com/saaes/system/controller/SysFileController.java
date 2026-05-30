package com.saaes.system.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.saaes.common.core.web.RestResponse;
import com.saaes.system.client.dto.FileUploadResponseDTO;
import com.saaes.system.client.entity.SysFile;
import com.saaes.system.service.SysFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

@Tag(name = "文件操作")
@RestController
@RequestMapping("/api/file/operation")
@Slf4j
public class SysFileController {

    @jakarta.annotation.Resource
    private SysFileService sysFileService;

    // @SaIgnore
    @Operation(description = "文件上传")
    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RestResponse<?> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return RestResponse.error("上传文件不能为空");
        }

        try {
            SysFile sysFile = sysFileService.uploadFile(file);
            return RestResponse.success(sysFile);
        } catch (Exception e) {
            log.error("文件上传失败", e);
            return RestResponse.error("文件上传失败: " + e.getMessage());
        }
    }

    // @SaIgnore
    @Operation(description = "新文件上传")
    @PostMapping(path = "/upload/v2", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RestResponse<?> uploadFileNew(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return RestResponse.error("上传文件不能为空");
        }

        try {
            SysFile sysFile = sysFileService.uploadFile(file);
            // 转换为简化的响应对象
            FileUploadResponseDTO responseDTO = new FileUploadResponseDTO();
            responseDTO.setName(sysFile.getName());
            responseDTO.setFileCode(sysFile.getFileCode());
            responseDTO.setUrl(sysFile.getUrl());
            responseDTO.setSize(sysFile.getSize());
            responseDTO.setContentType(sysFile.getContentType());
            responseDTO.setSuffix(sysFile.getSuffix());
            return RestResponse.success(responseDTO);
        } catch (Exception e) {
            log.error("文件上传失败", e);
            return RestResponse.error("文件上传失败: " + e.getMessage());
        }
    }

    @SaIgnore
    @Operation(description = "预览文件（支持视频流式播放）")
    @GetMapping("/preview/{fileCode}")
    public void previewFile(@PathVariable String fileCode, HttpServletRequest request, HttpServletResponse response) {
        log.info("接收到预览请求: {}", fileCode);
        handleFileRequestNative(fileCode, "inline", request, response);
    }

    @SaIgnore
    @Operation(description = "下载文件")
    @GetMapping("/download/{fileCode}")
    public void downloadFile(@PathVariable String fileCode, HttpServletRequest request, HttpServletResponse response) {
        log.info("接收到下载请求: {}", fileCode);
        handleFileRequestNative(fileCode, "attachment", request, response);
    }

//    @SaIgnore
//    @Operation(description = "文件下载（通过参数传递id）")
//    @GetMapping("/download")
//    public void downloadFileByParam(@RequestParam("id") String id, HttpServletRequest request, HttpServletResponse response) {
//        log.info("接收到下载请求（参数）: {}", id);
//        handleFileRequestNative(id, "attachment", request, response);
//    }
//
//    @SaIgnore
//    @Operation(description = "文件预览（通过参数传递id）")
//    @GetMapping("/preview")
//    public void previewFileByParam(@RequestParam("id") String id, HttpServletRequest request, HttpServletResponse response) {
//        log.info("接收到预览请求（参数）: {}", id);
//        handleFileRequestNative(id, "inline", request, response);
//    }

    /**
     * 处理文件请求的公共方法（原生方式，支持 Range 请求）
     */
    private void handleFileRequestNative(String idOrCode, String dispositionType, HttpServletRequest request, HttpServletResponse response) {
        try {
            Map<String, Object> fileInfo = sysFileService.getFileInfoForDownload(idOrCode);
            if (fileInfo == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            SysFile sysFile = (SysFile) fileInfo.get("sysFile");
            java.nio.file.Path filePath = (java.nio.file.Path) fileInfo.get("filePath");

            if (!Files.exists(filePath)) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            // 获取文件长度
            long contentLength = Files.size(filePath);

            // 获取 Range 请求头
            String rangeHeader = request.getHeader("Range");

            // 设置响应头
            response.setContentType(sysFile.getContentType());
            response.setHeader(HttpHeaders.ACCEPT_RANGES, "bytes");

            // 设置内容处置方式
            String contentDisposition = "inline";
            if ("attachment".equals(dispositionType)) {
                String encodedFilename = java.net.URLEncoder.encode(sysFile.getName(), StandardCharsets.UTF_8).replace("+", "%20");
                contentDisposition = "attachment; filename=\"" + encodedFilename + "\"; filename*=UTF-8''" + encodedFilename;
            }
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, contentDisposition);

            // 处理 Range 请求
            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                try {
                    // 解析 Range 请求头
                    String[] parts = rangeHeader.substring(6).split("-");
                    long start = Long.parseLong(parts[0]);
                    long end = parts.length > 1 && !parts[1].isEmpty() ? Long.parseLong(parts[1]) : contentLength - 1;

                    // 确保范围有效
                    end = Math.min(end, contentLength - 1);
                    if (start > end) {
                        response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                        return;
                    }

                    // 设置部分内容响应头
                    response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
                    response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + contentLength);
                    response.setContentLengthLong(end - start + 1);

                    // 写入部分内容
                    try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(filePath.toFile(), "r");
                         java.io.OutputStream os = response.getOutputStream()) {
                        raf.seek(start);
                        byte[] buffer = new byte[4096];
                        long remaining = end - start + 1;
                        int bytesRead;
                        while (remaining > 0 && (bytesRead = raf.read(buffer, 0, (int) Math.min(buffer.length, remaining))) != -1) {
                            os.write(buffer, 0, bytesRead);
                            os.flush();
                            remaining -= bytesRead;
                        }
                    }
                } catch (Exception e) {
                    log.warn("Range 解析失败: {}", rangeHeader);
                    response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                    return;
                }
            } else {
                // 返回完整文件
                response.setContentLengthLong(contentLength);
                try (java.io.InputStream is = Files.newInputStream(filePath);
                     java.io.OutputStream os = response.getOutputStream()) {
                    FileCopyUtils.copy(is, os);
                    os.flush();
                }
            }

        } catch (Exception e) {
            log.error("文件处理失败", e);
            if (!response.isCommitted()) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }


}
