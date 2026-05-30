package com.system.service.common.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.system.service.common.service.FileService;
import com.system.service.profile.constant.ProfileConstant;
import com.system.web.MyException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class FileServiceImpl implements FileService {

    // 最大文件大小设置为1000MB (1GB)
    @Value("${file.max-size:1073741824}") // 1000MB = 1000 * 1024 * 1024 bytes
    private long maxFileSize;

    @Override
    public String upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new MyException("上传文件不能为空");
        }

        // 检查文件大小
        if (file.getSize() > maxFileSize) {
            throw new MyException("文件大小不能超过 " + formatFileSize(maxFileSize));
        }

        // 1. 确保存储目录存在
        File storageDir = new File(ProfileConstant.FILE_UPLOAD_PATH);
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }

        try {
            log.info("开始上传文件: {}, 大小: {}",
                    file.getOriginalFilename(), formatFileSize(file.getSize()));

            // 2. 计算文件内容的MD5值用于去重
            String fileMd5 = calculateFileMd5(file);

            // 3. 检查是否已存在相同内容的文件
            String existingFile = findFileByMd5(fileMd5, storageDir);
            if (existingFile != null) {
                log.info("文件已存在，返回现有文件路径: {}", existingFile);
                return "/api/file/" + existingFile;
            }

            // 4. 生成新文件名 (MD5 + 原始后缀)
            String originalFilename = file.getOriginalFilename();
            String suffix = FileUtil.extName(originalFilename);
            String newFilename = fileMd5 + (StrUtil.isNotBlank(suffix) ? "." + suffix : "");

            // 5. 执行存储（使用流式处理避免内存溢出）
            File destFile = new File(storageDir, newFilename);

            try (InputStream inputStream = file.getInputStream();
                 FileOutputStream outputStream = new FileOutputStream(destFile)) {

                byte[] buffer = new byte[8192]; // 8KB缓冲区
                int bytesRead;
                long totalRead = 0;
                long lastLogTime = System.currentTimeMillis();

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    totalRead += bytesRead;

                    // 每秒记录一次进度（避免日志过多）
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastLogTime > 1000) {
                        double progress = (double) totalRead / file.getSize() * 100;
                        log.info("文件上传进度: {:.2f}% ({}/{})",
                                String.format("%.2f", progress),
                                formatFileSize(totalRead),
                                formatFileSize(file.getSize()));
                        lastLogTime = currentTime;
                    }
                }
                outputStream.flush();
            }

            log.info("文件上传成功: {} -> {}, 大小: {}",
                    originalFilename, newFilename, formatFileSize(file.getSize()));
            return "/api/file/" + newFilename;

        } catch (IOException e) {
            log.error("文件上传失败: ", e);
            throw new MyException("服务器保存文件失败");
        }
    }

    /**
     * 计算文件MD5（流式处理，避免大文件占用过多内存）
     */
    private String calculateFileMd5(MultipartFile file) throws IOException {
        try (InputStream inputStream = file.getInputStream()) {
            return DigestUtil.md5Hex(inputStream);
        }
    }

    /**
     * 根据MD5值查找已存在的文件
     */
    private String findFileByMd5(String md5, File storageDir) {
        File[] files = storageDir.listFiles();
        if (files == null) return null;

        for (File file : files) {
            if (file.isFile() && file.getName().startsWith(md5)) {
                return file.getName();
            }
        }
        return null;
    }

    @Override
    public void download(String fileName, HttpServletResponse response) {
        File file = new File(ProfileConstant.FILE_UPLOAD_PATH + File.separator + fileName);
        if (!file.exists()) {
            throw new MyException("文件不存在");
        }

        try (FileInputStream fis = new FileInputStream(file);
             BufferedOutputStream bos = new BufferedOutputStream(response.getOutputStream())) {

            response.addHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));
            response.setContentType("application/octet-stream");

            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalSent = 0;
            long lastLogTime = System.currentTimeMillis();

            while ((bytesRead = fis.read(buffer)) != -1) {
                bos.write(buffer, 0, bytesRead);
                totalSent += bytesRead;

                long currentTime = System.currentTimeMillis();
                if (currentTime - lastLogTime > 1000) {
                    log.info("文件下载进度: {} sent", formatFileSize(totalSent));
                    lastLogTime = currentTime;
                }
            }
            bos.flush();
        } catch (IOException e) {
            log.error("文件下载失败: ", e);
            throw new MyException("文件下载出错");
        }
    }

    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.2f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.2f MB", size / (1024.0 * 1024.0));
        return String.format("%.2f GB", size / (1024.0 * 1024.0 * 1024.0));
    }
}