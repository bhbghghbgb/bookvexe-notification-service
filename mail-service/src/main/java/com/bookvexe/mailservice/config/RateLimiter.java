package com.bookvexe.mailservice.config;

import java.util.List;

public class RateLimiter {
    private final List<RateLimitRule> rules;

    public RateLimiter(List<RateLimitRule> rules) {
        this.rules = rules;
    }

    public boolean canSend() {
        for (RateLimitRule rule : rules) {
            if (!rule.tryAcquire()) return false;
        }
        return true;
    }
}
