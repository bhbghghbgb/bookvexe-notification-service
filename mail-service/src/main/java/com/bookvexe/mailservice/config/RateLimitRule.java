package com.bookvexe.mailservice.config;

import java.util.ArrayDeque;
import java.util.Deque;

public class RateLimitRule {
    private final long windowMillis;
    private final int maxCount;
    private final Deque<Long> timestamps = new ArrayDeque<>();

    public RateLimitRule(long windowMillis, int maxCount) {
        this.windowMillis = windowMillis;
        this.maxCount = maxCount;
    }

    public synchronized boolean tryAcquire() {
        long now = System.currentTimeMillis();
        while (!timestamps.isEmpty() && timestamps.peekFirst() <= now - windowMillis) {
            timestamps.pollFirst();
        }
        if (timestamps.size() < maxCount) {
            timestamps.addLast(now);
            return true;
        }
        return false;
    }
}
