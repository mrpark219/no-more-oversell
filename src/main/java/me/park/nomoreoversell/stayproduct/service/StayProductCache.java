package me.park.nomoreoversell.stayproduct.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class StayProductCache {

    private static final Duration TTL = Duration.ofSeconds(10);
    private static final String KEY_PREFIX = "stay-product:";
    private static final String CIRCUIT_BREAKER_NAME = "redisCache";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;
    private final AsyncCacheWriter asyncCacheWriter;

    public Optional<StayProductView> get(Long stayProductId) {
        var circuitBreaker = circuitBreakerFactory.create(CIRCUIT_BREAKER_NAME);
        return circuitBreaker.run(
                () -> read(stayProductId),
                throwable -> {
                    log.debug("숙소 상품 캐시 fallback. stayProductId={}, cause={}", stayProductId, causeOf(throwable));
                    return Optional.empty();
                }
        );
    }

    public void put(StayProductView stayProduct) {
        asyncCacheWriter.execute("stayProductCache.put", () -> {
            var circuitBreaker = circuitBreakerFactory.create(CIRCUIT_BREAKER_NAME);
            circuitBreaker.run(
                    () -> {
                        write(stayProduct);
                        return null;
                    },
                    throwable -> {
                        log.debug("숙소 상품 캐시 저장 fallback. stayProductId={}, cause={}", stayProduct.id(), causeOf(throwable));
                        return null;
                    }
            );
        });
    }

    private Optional<StayProductView> read(Long stayProductId) {
        try {
            var cached = redisTemplate.opsForValue().get(key(stayProductId));
            if (cached == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(cached, StayProductView.class));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("숙소 상품 캐시를 역직렬화할 수 없습니다.", e);
        }
    }

    private void write(StayProductView stayProduct) {
        try {
            redisTemplate.opsForValue().set(
                    key(stayProduct.id()),
                    objectMapper.writeValueAsString(stayProduct),
                    TTL
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("숙소 상품 캐시를 직렬화할 수 없습니다.", e);
        }
    }

    private String key(Long stayProductId) {
        return KEY_PREFIX + stayProductId;
    }

    private String causeOf(Throwable throwable) {
        return throwable.getClass().getSimpleName();
    }
}
