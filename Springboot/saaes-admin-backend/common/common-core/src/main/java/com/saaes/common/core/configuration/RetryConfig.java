package com.saaes.common.core.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

@Configuration
@EnableRetry  // 启用 Spring Retry 功能
public class RetryConfig {

}
