package com.school.system.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.system.common.BusinessException;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

@Slf4j
@Component
public class StudentCatalogCache {
    static final String HOME_PAGE = "course:selectable:page:1:10";
    static final String DEPARTMENTS = "department:list";
    private static final Duration LOCK_LEASE = Duration.ofSeconds(8);
    private static final Duration LOCK_WAIT = Duration.ofMillis(350);
    private static final DefaultRedisScript<Long> FILL = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "redis.call('psetex', KEYS[2], ARGV[2], ARGV[3]); return 1 end return 0", Long.class);
    private static final DefaultRedisScript<Long> UNLOCK = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "return redis.call('del', KEYS[1]) end return 0", Long.class);

    private final StringRedisTemplate redis;
    private final ObjectMapper json;
    private final MeterRegistry metrics;
    private final boolean enabled;
    private final boolean homeEnabled;
    private final String prefix;
    private int consecutiveFailures;
    private long openedAt;

    public StudentCatalogCache(StringRedisTemplate redis, ObjectMapper json, MeterRegistry metrics,
            @Value("${app.cache.enabled}") boolean enabled,
            @Value("${app.cache.selectable-home-enabled}") boolean homeEnabled,
            @Value("${app.cache.environment}") String environment) {
        this.redis = redis;
        this.json = json;
        this.metrics = metrics;
        this.enabled = enabled;
        this.homeEnabled = homeEnabled;
        this.prefix = "school:" + environment + ":v1:";
    }

    <T> T get(String entry, TypeReference<T> type, int ttlSeconds, int jitterSeconds, Supplier<T> load) {
        if (!enabled || (HOME_PAGE.equals(entry) && !homeEnabled)) {
            return load.get();
        }
        if (circuitOpen()) {
            count(entry, "circuit_open");
            return load.get();
        }
        String key = prefix + entry;
        String lock = key + ":lock";
        String token = UUID.randomUUID().toString();
        try {
            T cached = read(key, type);
            if (cached != null) {
                return hit(entry, cached);
            }
            count(entry, "miss");
            if (!Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(lock, token, LOCK_LEASE))) {
                return waitForFill(entry, key, type);
            }
        } catch (DataAccessException e) {
            redisFailure(entry, e);
            return load.get();
        }

        try {
            try {
                T cached = read(key, type);
                if (cached != null) {
                    return hit(entry, cached);
                }
            } catch (DataAccessException e) {
                redisFailure(entry, e);
                return load.get();
            }

            T result = load.get();
            int ttl = ttlSeconds + ThreadLocalRandom.current().nextInt(-jitterSeconds, jitterSeconds + 1);
            try {
                redis.execute(FILL, List.of(lock, key), token, Long.toString(ttl * 1000L), json.writeValueAsString(result));
                redisSucceeded();
            } catch (DataAccessException e) {
                redisFailure(entry, e);
            } catch (JsonProcessingException e) {
                log.warn("无法编码学生目录缓存 {}", key, e);
            }
            return result;
        } finally {
            unlock(entry, lock, token);
        }
    }

    private <T> T read(String key, TypeReference<T> type) {
        String value = redis.opsForValue().get(key);
        if (value == null) {
            return null;
        }
        try {
            return json.readValue(value, type);
        } catch (JsonProcessingException e) {
            log.warn("无法读取学生目录缓存 {}", key, e);
            return null;
        }
    }

    private <T> T waitForFill(String entry, String key, TypeReference<T> type) {
        long started = System.nanoTime();
        try {
            do {
                Thread.sleep(ThreadLocalRandom.current().nextInt(20, 51));
                T cached = read(key, type);
                if (cached != null) {
                    return hit(entry, cached);
                }
            } while (System.nanoTime() - started < LOCK_WAIT.toNanos());
            redisSucceeded();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            metrics.timer("school.catalog.cache.wait", "entry", entry)
                    .record(Duration.ofNanos(System.nanoTime() - started));
        }
        count(entry, "busy");
        throw new BusinessException(503, "课程目录正在刷新，请稍后重试");
    }

    private <T> T hit(String entry, T cached) {
        redisSucceeded();
        count(entry, "hit");
        return cached;
    }

    private void unlock(String entry, String lock, String token) {
        try {
            redis.execute(UNLOCK, List.of(lock), token);
        } catch (DataAccessException e) {
            redisFailure(entry, e);
        }
    }

    @TransactionalEventListener
    public void invalidateHomePage(StudentCatalogChanged event) {
        if (!enabled) {
            return;
        }
        String key = prefix + HOME_PAGE;
        try {
            // 删除锁同时阻止提交前发起的查询在失效后回填旧值。
            redis.delete(List.of(key, key + ":lock"));
        } catch (DataAccessException e) {
            count(HOME_PAGE, "invalidation_error");
            log.warn("课程已提交，但首页缓存失效失败 {}", key, e);
        }
    }

    private synchronized boolean circuitOpen() {
        return consecutiveFailures >= 3 && System.nanoTime() - openedAt < Duration.ofSeconds(5).toNanos();
    }

    private synchronized void redisSucceeded() {
        if (!circuitOpen()) {
            consecutiveFailures = 0;
        }
    }

    private synchronized void redisFailure(String entry, DataAccessException failure) {
        count(entry, "redis_error");
        if (!circuitOpen() && ++consecutiveFailures >= 3) {
            openedAt = System.nanoTime();
            log.warn("Redis 缓存暂时熔断，学生目录将受限回源", failure);
        }
    }

    private void count(String entry, String outcome) {
        metrics.counter("school.catalog.cache", "entry", entry, "outcome", outcome).increment();
    }
}
