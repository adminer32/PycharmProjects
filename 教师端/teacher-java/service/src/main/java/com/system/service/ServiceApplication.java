package com.system.service;

import com.system.service.ai.config.DeepSeekConfig;
import com.system.service.common.config.PublicFileProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(scanBasePackages = {"com.system"})
@MapperScan("com.system.service.**.mapper")
@EnableConfigurationProperties({DeepSeekConfig.class, PublicFileProperties.class})
public class ServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceApplication.class, args);
    }

}
