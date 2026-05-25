package me.park.nomoreoversell.common.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import java.util.concurrent.RejectedExecutionException;

@Slf4j
@Component
public class AsyncCacheWriter {

    private final TaskExecutor taskExecutor;

    public AsyncCacheWriter(@Qualifier("cacheWriteTaskExecutor") TaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    public void execute(String operation, Runnable task) {
        try {
            taskExecutor.execute(() -> run(operation, task));
        } catch (RejectedExecutionException e) {
            log.debug("캐시 비동기 저장 큐 포화로 작업을 건너뜁니다. operation={}", operation);
        } catch (RuntimeException e) {
            log.warn("캐시 비동기 저장 등록 실패. operation={}", operation, e);
        }
    }

    private void run(String operation, Runnable task) {
        try {
            task.run();
        } catch (RuntimeException e) {
            log.warn("캐시 비동기 저장 실패. operation={}", operation, e);
        }
    }
}
