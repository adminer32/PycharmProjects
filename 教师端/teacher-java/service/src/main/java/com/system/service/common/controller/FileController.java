package com.system.service.common.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.system.service.common.service.FileService;
import com.system.web.RestResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URLConnection;
import java.nio.file.Path;
import java.nio.file.Paths;

@Tag(name = "通用文件管理")
@RestController
@RequestMapping("/api/file")
public class FileController {

    @Resource
    private FileService fileService;

    @Value("${public-file.paths:./uploads/files}")
    private String[] publicFilePaths;

    @Operation(description = "通用文件上传")
    @SaCheckRole("TEACHER")
    @PostMapping("/upload")
    public RestResponse<String> upload(@RequestParam("file") MultipartFile file) {
        String url = fileService.upload(file);
        return RestResponse.success("上传成功", url);
    }

    @Operation(description = "文件下载")
    @SaCheckRole("TEACHER")
    @GetMapping("/download/{fileName}")
    public void download(@PathVariable String fileName, HttpServletResponse response) {
        fileService.download(fileName, response);
    }

    @Operation(description = "公共文件访问（视频播放等，无需认证）")
    @GetMapping("/{fileName:.+}")
    public ResponseEntity<org.springframework.core.io.Resource> servePublicFile(
            @PathVariable String fileName,
            @RequestHeader(value = HttpHeaders.RANGE, required = false) String range) throws IOException {

        if (fileName.contains("..") || fileName.contains("\\")) {
            return ResponseEntity.badRequest().build();
        }

        File file = null;
        for (String rootPath : publicFilePaths) {
            Path filePath = Paths.get(rootPath, fileName).normalize();
            if (filePath.startsWith(Paths.get(rootPath).normalize())) {
                File candidate = filePath.toFile();
                if (candidate.exists() && candidate.isFile() && candidate.canRead()) {
                    file = candidate;
                    break;
                }
            }
        }

        if (file == null) {
            return ResponseEntity.notFound().build();
        }

        long fileLength = file.length();

        if (range != null && range.startsWith("bytes=")) {
            return handleRangeRequest(file, range, fileLength);
        }

        String contentType = java.nio.file.Files.probeContentType(file.toPath());
        if (contentType == null) {
            if (file.getName().toLowerCase().endsWith(".mp4")) {
                contentType = "video/mp4";
            } else {
                contentType = "application/octet-stream";
            }
        }


        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .contentLength(fileLength)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getName() + "\"")
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .body(new FileSystemResource(file));
    }

    private ResponseEntity<org.springframework.core.io.Resource> handleRangeRequest(File file, String range, long fileLength) throws IOException {
        String rangeValue = range.substring(6);
        
        long start;
        long end;
        
        if (rangeValue.startsWith("-")) {
            end = fileLength - 1;
            start = fileLength - Long.parseLong(rangeValue.substring(1));
        } else if (rangeValue.endsWith("-")) {
            start = Long.parseLong(rangeValue.substring(0, rangeValue.length() - 1));
            end = fileLength - 1;
        } else if (rangeValue.contains("-")) {
            String[] parts = rangeValue.split("-");
            start = Long.parseLong(parts[0]);
            end = parts.length > 1 && !parts[1].isEmpty() 
                ? Long.parseLong(parts[1]) 
                : fileLength - 1;
        } else {
            start = Long.parseLong(rangeValue);
            end = fileLength - 1;
        }

        if (start < 0) {
            start = 0;
        }
        if (end >= fileLength) {
            end = fileLength - 1;
        }
        if (start > end) {
            start = end;
        }

        long contentLength = end - start + 1;

        if (contentLength > 50 * 1024 * 1024) {
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
