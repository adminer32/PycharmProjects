package com.system.service.common.service;

import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;

public interface FileService {

    String upload(MultipartFile file);

    void download(String fileName, HttpServletResponse response);
}
