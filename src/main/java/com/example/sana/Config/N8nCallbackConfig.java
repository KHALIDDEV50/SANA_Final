package com.example.sana.Config;

import com.example.sana.Security.N8nCallbackFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

// Machine callback protection is independent of user authentication.
@Configuration
public class N8nCallbackConfig {
    @Bean
    public FilterRegistrationBean<N8nCallbackFilter> n8nCallbackRegistration(N8nProperties properties) {
        var registration = new FilterRegistrationBean<>(new N8nCallbackFilter(properties));
        registration.addUrlPatterns(N8nCallbackFilter.PATH);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
