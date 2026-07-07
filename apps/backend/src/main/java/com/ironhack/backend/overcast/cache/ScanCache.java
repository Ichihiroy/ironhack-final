package com.ironhack.backend.overcast.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis-backed cache for scan summaries and findings — the hot path the k6
 * load test hammers. Every operation degrades gracefully: if Redis is down
 * the caller falls back to the database and the app keeps working.
 */
@Component
public class ScanCache {

    private static final Logger log = LoggerFactory.getLogger(ScanCache.class);
    private static final Duration TTL = Duration.ofMinutes(15);

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;

    public ScanCache(StringRedisTemplate redis, ObjectMapper mapper) {
        this.redis = redis;
        this.mapper = mapper;
    }

    public <T> Optional<T> get(String key, Class<T> type) {
        try {
            String json = redis.opsForValue().get(key);
            if (json == null) return Optional.empty();
            return Optional.of(mapper.readValue(json, type));
        } catch (Exception e) {
            log.debug("Redis read miss ({}): {}", key, e.getMessage());
            return Optional.empty();
        }
    }

    public void put(String key, Object value) {
        try {
            redis.opsForValue().set(key, mapper.writeValueAsString(value), TTL);
        } catch (Exception e) {
            log.debug("Redis write skipped ({}): {}", key, e.getMessage());
        }
    }

    public void evictScan(String scanId) {
        try {
            redis.delete(summaryKey(scanId));
            redis.delete(findingsKey(scanId));
        } catch (Exception e) {
            log.debug("Redis evict skipped ({}): {}", scanId, e.getMessage());
        }
    }

    public static String summaryKey(String scanId) {
        return "scan:" + scanId + ":summary";
    }

    public static String findingsKey(String scanId) {
        return "scan:" + scanId + ":findings";
    }
}
