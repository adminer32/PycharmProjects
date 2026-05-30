package com.system;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j (SpringDoc) 配置类
 * 用于生成和管理 API 文档
 */
@Configuration
public class Knife4jConfig {

    /**
     * 配置 API 文档的基本信息
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("毽球教师端API文档")
                        .description("Spring Boot 3 的毽球接口文档")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("xxx")
                                .email("xxx@qq.com")));
    }

    /**
     * 业务接口分组
     */
    @Bean
    public GroupedOpenApi businessApi() {
        return GroupedOpenApi.builder()
                .group("教师端接口")
                .packagesToScan("com.system.service.auth.controller", "com.system.service.profile.controller")
                .pathsToMatch("/**")
                .build();
    }
}