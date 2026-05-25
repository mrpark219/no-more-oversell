package me.park.nomoreoversell.ordersheet.service;

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
public class CheckoutResponseCache {

    private static final Duration CHECKOUT_RESPONSE_TTL = Duration.ofSeconds(10);
    private static final String KEY_PREFIX = "checkout:response:";
    private static final String CIRCUIT_BREAKER_NAME = "redisCache";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;
    private final AsyncCacheWriter asyncCacheWriter;

    public Optional<CheckoutResponse> get(Long userId, Long stayProductId) {
        var circuitBreaker = circuitBreakerFactory.create(CIRCUIT_BREAKER_NAME);
        return circuitBreaker.run(
                () -> read(userId, stayProductId),
                throwable -> {
                    log.debug(
                            "체크아웃 응답 캐시 fallback. userId={}, stayProductId={}, cause={}",
                            userId,
                            stayProductId,
                            causeOf(throwable)
                    );
                    return Optional.empty();
                }
        );
    }

    public void put(Long userId, Long stayProductId, CheckoutResponse response) {
        asyncCacheWriter.execute("checkoutResponseCache.put", () -> {
            var circuitBreaker = circuitBreakerFactory.create(CIRCUIT_BREAKER_NAME);
            circuitBreaker.run(
                    () -> {
                        write(userId, stayProductId, response);
                        return null;
                    },
                    throwable -> {
                        log.debug(
                                "체크아웃 응답 캐시 저장 fallback. userId={}, stayProductId={}, cause={}",
                                userId,
                                stayProductId,
                                causeOf(throwable)
                        );
                        return null;
                    }
            );
        });
    }

    private Optional<CheckoutResponse> read(Long userId, Long stayProductId) {
        try {
            var cached = redisTemplate.opsForValue().get(key(userId, stayProductId));
            if (cached == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(cached, CheckoutResponse.class));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("체크아웃 응답 캐시를 역직렬화할 수 없습니다.", e);
        }
    }

    private void write(Long userId, Long stayProductId, CheckoutResponse response) {
        try {
            redisTemplate.opsForValue().set(
                    key(userId, stayProductId),
                    objectMapper.writeValueAsString(response),
                    CHECKOUT_RESPONSE_TTL
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("체크아웃 응답 캐시를 직렬화할 수 없습니다.", e);
        }
    }

    private String key(Long userId, Long stayProductId) {
        return KEY_PREFIX + userId + ":" + stayProductId;
    }

    private String causeOf(Throwable throwable) {
        return throwable.getClass().getSimpleName();
    }
}
