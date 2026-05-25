package me.park.nomoreoversell.common.cache;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskExecutor;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

class AsyncCacheWriterTest {

    @Test
    @DisplayName("캐시 저장 작업을 요청 스레드에서 바로 실행하지 않고 executor에 등록한다")
    void executeSubmitsTaskWithoutRunningInline() {
        // given
        var taskExecutor = new RecordingTaskExecutor();
        var writer = new AsyncCacheWriter(taskExecutor);
        var executed = new AtomicBoolean(false);

        // when
        writer.execute("cache.put", () -> executed.set(true));

        // then
        assertThat(executed).isFalse();
        assertThat(taskExecutor.task).isNotNull();

        taskExecutor.task.run();
        assertThat(executed).isTrue();
    }

    @Test
    @DisplayName("비동기 캐시 저장 실패가 요청 처리 흐름으로 전파되지 않는다")
    void executeDoesNotPropagateTaskFailure() {
        // given
        var taskExecutor = new RecordingTaskExecutor();
        var writer = new AsyncCacheWriter(taskExecutor);

        // when
        writer.execute("cache.put", () -> {
            throw new IllegalStateException("redis failed");
        });

        // then
        assertThatNoException()
                .isThrownBy(() -> taskExecutor.task.run());
    }

    @Test
    @DisplayName("캐시 저장 executor가 포화되어도 요청 처리 흐름으로 전파되지 않는다")
    void executeDoesNotPropagateRejectedExecution() {
        // given
        var taskExecutor = new RejectedTaskExecutor();
        var writer = new AsyncCacheWriter(taskExecutor);

        // when & then
        assertThatNoException()
                .isThrownBy(() -> writer.execute("cache.put", () -> {
                }));
    }

    private static class RecordingTaskExecutor implements TaskExecutor {

        private Runnable task;

        @Override
        public void execute(Runnable task) {
            this.task = task;
        }
    }

    private static class RejectedTaskExecutor implements TaskExecutor {

        @Override
        public void execute(Runnable task) {
            throw new RejectedExecutionException("queue is full");
        }
    }
}
