package com.system.service.profile.configuration;

import com.system.service.common.config.PublicFileProperties;
import com.system.service.profile.constant.ProfileConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private PublicFileProperties publicFileProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String imagePath = "file:" + ProfileConstant.IMAGE_UPLOAD_PATH + File.separator;
        registry.addResourceHandler(ProfileConstant.RESOURCE_HANDLER)
                .addResourceLocations(imagePath);
        
        String filePath = "file:" + ProfileConstant.FILE_UPLOAD_PATH + File.separator;
        registry.addResourceHandler(ProfileConstant.FILE_RESOURCE_HANDLER)
                .addResourceLocations(filePath);

        String[] locations = publicFileProperties.getPathsArray();
        String[] fileLocations = new String[locations.length];
        
        for (int i = 0; i < locations.length; i++) {
            String path = locations[i];
            if (!path.startsWith("file:")) {
                fileLocations[i] = "file:" + path + (path.endsWith("/") || path.endsWith("\\") ? "" : "/");
            } else {
                fileLocations[i] = path;
            }
        }
        
        registry.addResourceHandler("/videos/**")
                .addResourceLocations(fileLocations)
                .setCachePeriod(3600);
    }
}
