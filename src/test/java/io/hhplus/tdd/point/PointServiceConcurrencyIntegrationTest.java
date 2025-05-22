package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

class PointServiceConcurrencyIntegrationTest {

    private PointService pointService;

    @BeforeEach
    void setUp() {
        UserPointTable userPointTable = new UserPointTable();
        PointHistoryTable pointHistoryTable = new PointHistoryTable();
        pointService = new PointService(userPointTable, pointHistoryTable);
    }

    @Test
    @DisplayName("동시에 여러 충전 요청이 들어올 경우에도 포인트 충전은 정확히 반영된다.")
    void chargeUserPoint_concurrency() throws InterruptedException {
        // given
        long id = 1L;
        int threadCount = 100;
        long amount = 10L;
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(10);

        // when - 100개의 충전 요청을 동시에 실행
        for (int i = 0; i < threadCount; i++) {
            CompletableFuture.runAsync(() -> {
                pointService.chargeUserPoint(id, amount);
                latch.countDown();
            }, executor);
        }
        latch.await();
        
        // then
        UserPoint result = pointService.getUserPoint(id);
        assertThat(result.point()).isEqualTo(amount * threadCount);

        List<PointHistory> userPointHistory = pointService.getUserPointHistory(id);
        assertThat(userPointHistory).hasSize(threadCount);
        userPointHistory.forEach(history -> assertThat(history.amount()).isEqualTo(amount));
    }


    @Test
    @DisplayName("동시에 여러 요청이 들어와도 포인트 사용은 모두 반영된다")
    void useUserPoint_concurrency() throws InterruptedException {
        // given
        long id = 1L;
        int threadCount = 100;
        long amount = 10L;
        pointService.chargeUserPoint(id, amount * threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(10);
        
        // when - 100개의 10 포인트 사용 요청을 동시에 실행
        for (int i = 0; i < threadCount; i++) {
            CompletableFuture.runAsync(() -> {
                try {
                    pointService.useUserPoint(id, amount);
                } finally {
                    latch.countDown();
                }
            }, executor);
        }
        latch.await();

        // then
        UserPoint result = pointService.getUserPoint(id);
        assertThat(result.point()).isZero();
    }

    @Test
    @DisplayName("잔액이 부족할 때 포인트 사용 요청이 들어올 경우 실패하는 경우도 존재한다")
    void useUserPoint_concurrency_withInsufficientPoint() throws InterruptedException {
        // given
        long id = 1L;
        long initialPoint = 100L;
        int threadCount = 10;
        long amount = 20L;
        pointService.chargeUserPoint(id, initialPoint);

        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(5);

        int[] successCount = {0};

        // when
        for (int i = 0; i < threadCount; i++) {
            CompletableFuture.runAsync(() -> {
                try {
                    pointService.useUserPoint(id, amount);
                    synchronized (successCount){
                        successCount[0]++;
                    }
                } catch (IllegalArgumentException e) {
                    // 잔고 부족은 무시
                } finally {
                    latch.countDown();
                }
            }, executor);
        }
        latch.await();

        // then
        assertThat(successCount[0]).isEqualTo(5);
        UserPoint result = pointService.getUserPoint(id);
        assertThat(result.point()).isZero();
    }
}
