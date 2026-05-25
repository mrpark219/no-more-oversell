package me.park.nomoreoversell.point.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.park.nomoreoversell.common.cache.AsyncCacheWriter;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PointBalanceCache {

    private static final Duration TTL = Duration.ofSeconds(5);
    private static final String KEY_PREFIX = "point:balance:";
    private static final String CIRCUIT_BREAKER_NAME = "redisCache";

    private final StringRedisTemplate redisTemplate;
    private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;
    private final AsyncCacheWriter asyncCacheWriter;

    public Optional<Long> get(Long userId) {
        var circuitBreaker = circuitBreakerFactory.create(CIRCUIT_BREAKER_NAME);
        return circuitBreaker.run(
                () -> read(userId),
                throwable -> {
                    log.debug("포인트 잔액 캐시 fallback. userId={}, cause={}", userId, causeOf(throwable));
                    return Optional.empty();
                }
        );
    }

    public void put(Long userId, long balance) {
        asyncCacheWriter.execute("pointBalanceCache.put", () -> {
            var circuitBreaker = circuitBreakerFactory.create(CIRCUIT_BREAKER_NAME);
            circuitBreaker.run(
                    () -> {
                        redisTemplate.opsForValue().set(key(userId), String.valueOf(balance), TTL);
                        return null;
                    },
                    throwable -> {
                        log.debug("포인트 잔액 캐시 저장 fallback. userId={}, cause={}", userId, causeOf(throwable));
                        return null;
                    }
            );
        });
    }

    public void evict(Long userId) {
        var circuitBreaker = circuitBreakerFactory.create(CIRCUIT_BREAKER_NAME);
        circuitBreaker.run(
                () -> {
                    redisTemplate.delete(key(userId));
                    return null;
                },
                throwable -> {
                    log.debug("포인트 잔액 캐시 삭제 fallback. userId={}, cause={}", userId, causeOf(throwable));
                    return null;
                }
        );
    }

    private Optional<Long> read(Long userId) {
        var cached = redisTemplate.opsForValue().get(key(userId));
        if (cached == null) {
            return Optional.empty();
        }
        return Optional.of(Long.parseLong(cached));
    }

    private String key(Long userId) {
        return KEY_PREFIX + userId;
    }

    private String causeOf(Throwable throwable) {
        return throwable.getClass().getSimpleName();
    }
}
