package com.system.complaints.service;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimiter {

    private final Map<Long, LocalDateTime> lastUpdateTime = new ConcurrentHashMap<>();
    private final int RATE_LIMIT_SECONDS = 5;

    public boolean canUpdate(Long visitorId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastTime = lastUpdateTime.get(visitorId);

        if (lastTime == null || Duration.between(lastTime, now).getSeconds() >= RATE_LIMIT_SECONDS) {
            lastUpdateTime.put(visitorId, now);
            return true;
        }
        return false;
    }
}
