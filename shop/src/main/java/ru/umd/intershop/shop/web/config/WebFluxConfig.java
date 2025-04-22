package ru.umd.intershop.shop.web.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.ResourceHandlerRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;

import java.io.File;

@Configuration
public class WebFluxConfig implements WebFluxConfigurer {

    @Value("${app.image-file-base-path}")
    private String imageBasePath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/images/**")
            .addResourceLocations("file:" + imageBasePath + File.separator);
    }
}