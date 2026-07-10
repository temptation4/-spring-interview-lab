package com.interview.labs.mvc.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private LoggingInterceptor interceptor;

    @Override
    public void addInterceptors(
            InterceptorRegistry registry) {

        registry.addInterceptor(interceptor);
    }
}
