package com.saaes.common.swagger.configuration;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.SpecVersion;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

/**
 * 配置swagger
 */
public class SpringDocConfig {
    @Value("${sys.auth.tokenHeaderName}")
    private String tokenHeaderName;

    @Bean
    public OpenAPI myOpenAPI() {
        return new OpenAPI()
                .specVersion(SpecVersion.V31)
                .info(new Info()
                        .title("毽球动作自动化评估系统API")
                        .version("v0.1.0")
                        .license(new License().name("Apache 2.0").url("http://springdoc.org")))
                .externalDocs(new ExternalDocumentation()
                        .description("后台入口")
                        .url("#")
                )
                //需要授权
                .addSecurityItem(
                        new SecurityRequirement().addList(tokenHeaderName)
                )
                .schemaRequirement(tokenHeaderName,
                        new SecurityScheme()
                                .name(tokenHeaderName)
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER));
    }
}
