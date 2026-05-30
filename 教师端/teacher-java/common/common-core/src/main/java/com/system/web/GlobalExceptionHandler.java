package com.system.web;

import cn.dev33.satoken.exception.NotLoginException;
import jakarta.servlet.ServletException;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.IOException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 拦截客户端断开连接异常（视频流/文件下载时常见）
     * 客户端主动断开连接，无需返回响应，只需记录日志
     */
    @ExceptionHandler(ClientAbortException.class)
    public void handleClientAbortException(ClientAbortException e) {
        log.debug("客户端断开连接: {}", e.getMessage());
    }

    /**
     * 拦截 Servlet 异常（包含客户端断开的情况）
     */
    @ExceptionHandler(ServletException.class)
    public void handleServletException(ServletException e) {
        if (e.getMessage() != null && e.getMessage().contains("Connection reset")) {
            log.debug("客户端重置连接: {}", e.getMessage());
        } else {
            log.warn("Servlet异常: {}", e.getMessage());
        }
    }

    /**
     * 拦截 IO 异常（客户端断开时会触发）
     */
    @ExceptionHandler(IOException.class)
    public void handleIOException(IOException e) {
        String message = e.getMessage();
        if (message != null && (message.contains("Connection reset") 
                || message.contains("Broken pipe")
                || message.contains("远程主机强迫关闭"))) {
            log.debug("客户端断开连接: {}", message);
        } else {
            log.error("IO异常: ", e);
        }
    }

    /**
     * 拦截 Sa-Token 未登录异常（Token 过期或无效）
     */
    @ExceptionHandler(NotLoginException.class)
    public org.springframework.http.ResponseEntity<RestResponse<String>> handleNotLoginException(NotLoginException e) {
        log.warn("用户未登录或Token已过期: {}", e.getMessage());
        return org.springframework.http.ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(RestResponse.error("登录已失效，请重新登录"));
    }

    /**
     * 拦截业务自定义异常
     */
    @ExceptionHandler(MyException.class)
    public RestResponse<String> handleMyException(MyException e) {
        log.debug("业务异常被拦截: {}", e.getMessage());
        return RestResponse.error(e.getMessage());
    }

    /**
     * 拦截静态资源找不到异常（如 favicon.ico）
     * 不打印错误日志，避免干扰
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public void handleNoResourceFoundException(NoResourceFoundException e) {
        log.debug("静态资源未找到: {}", e.getResourcePath());
    }

    /**
     * 拦截其他未知的系统异常
     */
    @ExceptionHandler(Exception.class)
    public RestResponse<String> handleException(Exception e) {
        String message = e.getMessage();
        if (message != null && (message.contains("Connection reset") 
                || message.contains("Broken pipe")
                || message.contains("AsyncRequestNotUsable"))) {
            log.debug("客户端连接异常: {}", message);
            return null;
        }
        log.error("系统异常被拦截: ", e);
        return RestResponse.error("服务器内部错误，请稍后重试");
    }
}
