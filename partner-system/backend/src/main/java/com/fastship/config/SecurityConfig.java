package com.fastship.config;

import com.fastship.security.ApiKeyFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecurityConfig {

    @Bean
    public FilterRegistrationBean<ApiKeyFilter> apiKeyFilter(@Value("${fastship.api-key}") String apiKey) {
        FilterRegistrationBean<ApiKeyFilter> bean = new FilterRegistrationBean<>(new ApiKeyFilter(apiKey));
        bean.addUrlPatterns("/api/v1/*");
        bean.setOrder(1);
        return bean;
    }
}
