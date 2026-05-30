package com.system.service.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "public-file")
public class PublicFileProperties {

    private List<String> paths;

    public String[] getPathsArray() {
        if (paths == null || paths.isEmpty()) {
            return new String[]{"./uploads/files"};
        }
        return paths.toArray(new String[0]);
    }
}
