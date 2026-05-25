package me.park.nomoreoversell.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class CacheWriteExecutorConfig {

    @Bean
    public ThreadPoolTaskExecutor cacheWriteTaskExecutor(
            @Value("${cache.write.executor.core-size:2}") int coreSize,
            @Value("${cache.write.executor.max-size:4}") int maxSize,
            @Value("${cache.write.executor.queue-capacity:1000}") int queueCapacity
    ) {
        var executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("cache-write-");
        executor.setCorePoolSize(coreSize);
        executor.setMaxPoolSize(maxSize);
        executor.setQueueCapacity(queueCapacity);
        // 캐시는 조회 최적화 힌트이므로 큐가 가득 차면 요청을 막지 않고 저장 작업만 버린다.
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(false);
        return executor;
    }
}
