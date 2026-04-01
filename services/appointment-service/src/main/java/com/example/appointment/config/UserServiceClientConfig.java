package com.example.appointment.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class UserServiceClientConfig {

    @Bean(name = "userServiceRestClient")
    public RestClient userServiceRestClient(@Value("${app.user-service.base-url}") String baseUrl) {
        return RestClient.builder().baseUrl(baseUrl).build();
    }
}
