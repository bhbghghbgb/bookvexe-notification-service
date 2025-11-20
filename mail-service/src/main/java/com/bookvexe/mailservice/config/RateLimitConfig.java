package com.bookvexe.mailservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class RateLimitConfig {

    @Bean
    public RateLimiter rateLimiter() {
        return new RateLimiter(List.of(
            new RateLimitRule(5000, 1),     // 1 per 5s
            new RateLimitRule(60000, 5),    // 5 per min
            new RateLimitRule(3600000, 50),  // 50 per hour
            new RateLimitRule(86400000, 100) // daily limit
        ));
    }
}
