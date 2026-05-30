package com.system.service.common.controller;

import com.system.service.common.config.PublicFileProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URLConnection;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
@Tag(name = "公共文件读取接口")
public class PublicFileController {

    private final PublicFileProperties publicFileProperties;
    private String[] publicFilePaths;

    @PostConstruct
    public void init() {
        publicFilePaths = publicFileProperties.getPathsArray();
        log.info("=== PublicFileController 初始化 ===");
        log.info("配置的公共文件路径数量: {}", publicFilePaths.length);
        for (int i = 0; i < publicFilePaths.length; i++) {
            log.info("路径[{}]: {}", i, publicFilePaths[i]);
        }
    }

    @GetMapping("/file/{fileName:.+}")
    @Operation(description = "公共文件服务")
    public ResponseEntity<Resource> serveFile(
            @PathVariable String fileName,
            @RequestHeader(value = HttpHeaders.RANGE, required = false) String range) throws IOException {

        log.info("请求文件: {}", fileName);

        if (fileName.contains("..") || fileName.contains("\\")) {
            log.warn("文件名包含非法字符: {}", fileName);
            return ResponseEntity.badRequest().build();
        }

        File file = null;
        for (String rootPath : publicFilePaths) {
            Path filePath = Paths.get(rootPath, fileName).normalize();
            log.info("尝试路径: {} -> {}", rootPath, filePath.toAbsolutePath());
            if (filePath.startsWith(Paths.get(rootPath).normalize())) {
                File candidate = filePath.toFile();
                log.info("检查文件: exists={}, isFile={}, canRead={}", 
                    candidate.exists(), candidate.isFile(), candidate.canRead());
                if (candidate.exists() && candidate.isFile() && candidate.canRead()) {
                    file = candidate;
                    log.info("找到文件: {}", file.getAbsolutePath());
                    break;
                }
            }
        }

        if (file == null) {
            log.warn("文件未找到: {}", fileName);
            return ResponseEntity.notFound().build();
        }

        long fileLength = file.length();

        if (range != null && range.startsWith("bytes=")) {
            return handleRangeRequest(file, range, fileLength);
        }

        String contentType = URLConnection.guessContentTypeFromName(file.getName());
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .contentLength(fileLength)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getName() + "\"")
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .body(new FileSystemResource(file));
    }

    private ResponseEntity<Resource> handleRangeRequest(File file, String range, long fileLength) throws IOException {
        String[] ranges = range.substring(6).split("-", -1);
        long start = Long.parseLong(ranges[0]);
        long end = ranges[1].isEmpty() ? fileLength - 1 : Long.parseLong(ranges[1]);

        if (end >= fileLength) {
            end = fileLength - 1;
        }

        long contentLength = end - start + 1;

        if (contentLength > 50 * 1024 * 1024) {
            log.info("大文件 Range 请求，使用流式传输: {} bytes", contentLength);
            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                    .contentType(MediaType.parseMediaType(URLConnection.guessContentTypeFromName(file.getName()) != null 
                            ? URLConnection.guessContentTypeFromName(file.getName()) : "application/octet-stream"))
                    .contentLength(contentLength)
                    .header(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + fileLength)
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .body(new FileSystemResource(file));
        }

        byte[] data = new byte[(int) contentLength];
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            raf.seek(start);
            raf.readFully(data);
        } catch (IOException e) {
            if (e.getMessage() != null && (e.getMessage().contains("Connection reset") 
                    || e.getMessage().contains("Broken pipe"))) {
                log.debug("客户端断开连接，Range请求中止");
                return null;
            }
            throw e;
        }

        String contentType = URLConnection.guessContentTypeFromName(file.getName());

        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .contentType(MediaType.parseMediaType(contentType != null ? contentType : "application/octet-stream"))
                .contentLength(contentLength)
                .header(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + fileLength)
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .body(new ByteArrayResource(data));
    }
}
