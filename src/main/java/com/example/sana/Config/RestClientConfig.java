package com.example.sana.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    // Provide a builder for services that call external APIs.
    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}