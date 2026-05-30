package com.saaes.common.core.configuration;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.context.SaTokenContext;
import cn.dev33.satoken.router.SaRouter;
import com.alibaba.fastjson2.JSONObject;
import com.saaes.common.core.entity.SysLog;
import com.saaes.common.core.service.CommonService;
import com.saaes.common.core.utils.CommonUtil;
import com.saaes.common.core.web.MyContext;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * web过滤器
 *
 */
@Configuration
@Slf4j
public class MyFilter extends HttpFilter {

    @Resource
    private CommonService commonService;
    // 实例化一个路径匹配器，线程安全
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    // 定义不需要记录日志的路径白名单
    private static final List<String> IGNORE_LOG_PATHS = Arrays.asList(
            "/api/system/log/get/**",
            "/api/system/user/queryOnlineUser",
            "/api/file/operation/download",
            "/api/system/user/queryUserGroupList"
    );
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException {
        // 放行 CORS 预检请求，避免 No mapping for OPTIONS 错误
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            response.setHeader("Access-Control-Allow-Origin", "*");
            response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            response.setHeader("Access-Control-Allow-Headers", "*");
            response.setHeader("Access-Control-Max-Age", "3600");
            return;
        }
        preHandle(request);
        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        try {
            chain.doFilter(requestWrapper, responseWrapper);
        } catch (Exception e) {
            log.error("请求异常", e);
            MyContext.getSysLog().setStackTrace(CommonUtil.getThrowString(e));
        }
        afterHandle(requestWrapper, responseWrapper);
    }

    /**
     * 前置处理
     */
    private void preHandle(HttpServletRequest request) {
        //获取真实IP
        String ip = request.getHeader("X-Real-IP");
        if (CommonUtil.isEmpty(ip)) {
            ip = request.getRemoteAddr();
        }
    }

    /**
     * 后置处理
     */
    private void afterHandle(ContentCachingRequestWrapper request, ContentCachingResponseWrapper response) throws IOException {
        SysLog sysLog = MyContext.getSysLog();
        ServletInputStream inputStream = request.getRequest().getInputStream();
        //如果文件流已读取则从缓存中获取请求体
        if (Boolean.TRUE.equals(inputStream.isFinished())) {
            sysLog.setRequestBody(request.getContentAsString());
        } else {
            // 否则直接从request中获取请求体
            String requestBody = new String(inputStream.readAllBytes());
            sysLog.setRequestBody(requestBody);
        }
        //存储响应体内容
        String contentType = response.getContentType();
        if (contentType != null && contentType.startsWith("application/json")) {
            byte[] contentAsByteArray = response.getContentAsByteArray();
            String responseContent = new String(contentAsByteArray);
            sysLog.setResponseBody(responseContent);
        }
        response.copyBodyToResponse();

        String uri = request.getRequestURI();
        String method = request.getMethod();

        // 默认需要记录日志 (hit = true)
        boolean hit = true;

        // 1. 如果是 OPTIONS 请求，不记录
        if ("OPTIONS".equalsIgnoreCase(method)) {
            hit = false;
        }
        // 2. 如果 URI 以 /query 结尾，不记录
        else if (uri.endsWith("/query")) {
            hit = false;
        }
        // 3. 检查白名单路径
        else {
            for (String pattern : IGNORE_LOG_PATHS) {
                if (PATH_MATCHER.match(pattern, uri)) {
                    hit = false;
                    break;
                }
            }
        }
        //设置指定匹配的或者出现报错的才记录日志
        if (hit || sysLog.getStackTrace() != null) {
            final SaTokenContext saTokenContextOrSecond = SaManager.getSaTokenContextOrSecond();
            //异步存储请求的日志信息
            Mono.just(sysLog).subscribe(i -> {
                // 因为会开启新线程，所以把上SaToken下文对象传递进来
                if (saTokenContextOrSecond != null) {
                    SaManager.setSaTokenContext(saTokenContextOrSecond);
                }
//                commonService.saveSysLog(i);
            });
        }
    }
}
