package com.saaes.common.core.utils;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

public class HttpUtil {

    @Retryable(
            value = { Exception.class },      // 发生异常时会进行重试
            maxAttempts = 3,                 // 最大重试次数
            backoff = @Backoff(delay = 1000) // 每次重试间隔 1 秒
    )
    public static String sendHttpRequestWithJson(String postUrl, String postJson) {
        HttpResponse response = HttpRequest.post(postUrl)
                .header("Content-Type", "application/json; charset=UTF-8")
                .contentType("application/json")
                .body(postJson)
                .execute();

        return response.body();  // 返回响应内容
    }
}
