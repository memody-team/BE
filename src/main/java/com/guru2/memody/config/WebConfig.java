package com.guru2.memody.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${image.storage}")
    private String imageStoragePath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 1. 변수(imageStoragePath)를 사용해야 함
        // 2. 로컬 디스크 경로라면 반드시 "file:///" 접두어가 필요함
        // 3. 경로 끝에 "/"가 없으면 붙여줘야 함

        String resourceLocation = imageStoragePath;

        // 경로 보정 로직 (file: 접두어 및 끝 슬래시 처리)
        if (!resourceLocation.startsWith("file:")) {
            if (resourceLocation.startsWith("/")) {
                // 맥/리눅스 (/Users/...) -> file:///Users/...
                resourceLocation = "file://" + resourceLocation;
            } else {
                // 윈도우 (C:/...) -> file:///C:/...
                resourceLocation = "file:///" + resourceLocation;
            }
        }

        if (!resourceLocation.endsWith("/")) {
            resourceLocation += "/";
        }

        registry.addResourceHandler("/uploads/images/**")
                .addResourceLocations(resourceLocation);
    }
}