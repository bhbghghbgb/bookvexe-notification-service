package com.bookvexe.mailservice.config;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RateLimiter {

    private final List<RateLimitRule> rules;

    public RateLimiter() {
        rules = List.of(
            new RateLimitRule(5000, 1),       // max 1 per 5s
            new RateLimitRule(60000, 5),      // max 5 per minute
            new RateLimitRule(3600000, 50)    // max 50 per hour
        );
    }

    public boolean canSend() {
        for (RateLimitRule rule : rules) {
            if (!rule.tryAcquire()) {
                return false;
            }
        }
        return true;
    }
}
