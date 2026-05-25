package me.park.nomoreoversell.inventory.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.park.nomoreoversell.common.cache.AsyncCacheWriter;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductSoldOutCache {

    private static final Duration TTL = Duration.ofSeconds(10);
    private static final String KEY_PREFIX = "product:soldout:";
    private static final String CIRCUIT_BREAKER_NAME = "redisCache";

    private final StringRedisTemplate redisTemplate;
    private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;
    private final AsyncCacheWriter asyncCacheWriter;

    public boolean isSoldOut(Long productId) {
        var circuitBreaker = circuitBreakerFactory.create(CIRCUIT_BREAKER_NAME);
        return circuitBreaker.run(
                () -> redisTemplate.hasKey(key(productId)),
                throwable -> {
                    log.debug("상품 마감 힌트 캐시 fallback. productId={}, cause={}", productId, causeOf(throwable));
                    return false;
                }
        );
    }

    public void markSoldOut(Long productId) {
        asyncCacheWriter.execute("productSoldOutCache.markSoldOut", () -> {
            var circuitBreaker = circuitBreakerFactory.create(CIRCUIT_BREAKER_NAME);
            circuitBreaker.run(
                    () -> {
                        redisTemplate.opsForValue().set(key(productId), "true", TTL);
                        return null;
                    },
                    throwable -> {
                        log.debug("상품 마감 힌트 캐시 저장 fallback. productId={}, cause={}", productId, causeOf(throwable));
                        return null;
                    }
            );
        });
    }

    public void evict(Long productId) {
        var circuitBreaker = circuitBreakerFactory.create(CIRCUIT_BREAKER_NAME);
        circuitBreaker.run(
                () -> {
                    redisTemplate.delete(key(productId));
                    return null;
                },
                throwable -> {
                    log.debug("상품 마감 힌트 캐시 삭제 fallback. productId={}, cause={}", productId, causeOf(throwable));
                    return null;
                }
        );
    }

    private String key(Long productId) {
        return KEY_PREFIX + productId;
    }

    private String causeOf(Throwable throwable) {
        return throwable.getClass().getSimpleName();
    }
}
