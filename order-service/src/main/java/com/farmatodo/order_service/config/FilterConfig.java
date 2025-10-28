package com.farmatodo.order_service.config;

import com.farmatodo.order_service.filter.ApiKeyAuthFilter;
import com.farmatodo.order_service.filter.MdcFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterConfig {

    @Value("${api.key}")
    private String validApiKey;

    @Bean
    public MdcFilter mdcFilter() {
        return new MdcFilter();
    }

    @Bean
    public ApiKeyAuthFilter apiKeyAuthFilter() {
        return new ApiKeyAuthFilter(validApiKey);
    }

    @Bean
    public FilterRegistrationBean<MdcFilter> mdcFilterRegistration(MdcFilter mdcFilter) {
        FilterRegistrationBean<MdcFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(mdcFilter);
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(1); // MDC filter runs first
        return registrationBean;
    }

    @Bean
    public FilterRegistrationBean<ApiKeyAuthFilter> apiKeyAuthFilterRegistration(ApiKeyAuthFilter apiKeyAuthFilter) {
        FilterRegistrationBean<ApiKeyAuthFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(apiKeyAuthFilter);
        registrationBean.addUrlPatterns("/orders/*");
        registrationBean.setOrder(2); // API Key filter runs second
        return registrationBean;
    }
}
