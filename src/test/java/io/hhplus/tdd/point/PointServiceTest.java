package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class PointServiceTest {

    private UserPointTable userPointTable;
    private PointHistoryTable historyTable;
    private PointService pointService;

    @BeforeEach
    void setUp() {
        userPointTable = mock(UserPointTable.class);
        historyTable = mock(PointHistoryTable.class);
        pointService = new PointService(userPointTable, historyTable);
    }

    @Test
    @DisplayName("특정 유저의 포인트를 조회합니다.")
    void getUserPoint_ReturnValidId() {
        // given
        long id = 1L;

        when(userPointTable.selectById(id)).thenReturn(new UserPoint(id, 100L, System.currentTimeMillis()));

        // when
        UserPoint userPoint = pointService.getUserPoint(id);

        // then
        assertThat(userPoint.id()).isEqualTo(id);
        assertThat(userPoint.point()).isEqualTo(100L);
        verify(userPointTable, times(1)).selectById(id);
    }

    @Test
    @DisplayName("특정 유저의 포인트 충전을 성공하고 히스토리를 저장한다.")
    void chargeUserPoint_withValidUserIdAndPoint_AndSaveHistory() {
        // given
        long id = 1L;
        long beforePoint = 100L;    // 업데이트 전 포인트
        long amount = 100L;         // 충전 포인트
        long updatedPoint = 200L;   // 업데이트 후 포인트
        long currentTimeMillis = System.currentTimeMillis();

        when(userPointTable.selectById(id)).thenReturn(new UserPoint(id, beforePoint, currentTimeMillis));
        when(userPointTable.insertOrUpdate(id, updatedPoint))
                .thenReturn(new UserPoint(id, updatedPoint, currentTimeMillis));

        // when
        UserPoint userPoint = pointService.chargeUserPoint(id, amount);

        // then
        assertThat(userPoint.id()).isEqualTo(id);
        assertThat(userPoint.point()).isEqualTo(updatedPoint);

        verify(userPointTable, times(1)).selectById(id);
        verify(userPointTable, times(1)).insertOrUpdate(id, updatedPoint);
        verify(historyTable, times(1)).insert(id, amount, TransactionType.CHARGE, currentTimeMillis);
    }

    @Test
    @DisplayName("특정 유저의 포인트 충전이 실패합니다(최대 잔고 초과).")
    void chargeUserPoint_withOverPoint() {
        // given
        long id = 1L;
        long beforePoint = 1L;
        long amount = Long.MAX_VALUE;
        long currentTimeMillis = System.currentTimeMillis();

        when(userPointTable.selectById(id)).thenReturn(new UserPoint(id, beforePoint, currentTimeMillis));

        // when & then
        assertThatThrownBy(() -> pointService.chargeUserPoint(id, amount))
                .isInstanceOf(IllegalArgumentException.class);

        verify(historyTable, never()).insert(id, amount, TransactionType.CHARGE, currentTimeMillis);
    }

    @Test
    @DisplayName("특정 유저의 포인트를 사용을 성공합니다.")
    void useUserPoint_withValidUserIdAndPoint() {
        // given
        long id = 1L;
        long amount = 100L;
        long useAmount = 100L;
        long currentTimeMillis = System.currentTimeMillis();

        when(userPointTable.selectById(id)).thenReturn(new UserPoint(id, amount, currentTimeMillis));
        when(userPointTable.insertOrUpdate(id, 0L)).thenReturn(new UserPoint(id, 0L, currentTimeMillis));

        // when
        UserPoint userPoint = pointService.useUserPoint(id, useAmount);

        // then
        assertThat(userPoint.id()).isEqualTo(id);
        assertThat(userPoint.point()).isZero();

        verify(userPointTable, times(1)).selectById(id);
        verify(userPointTable, times(1)).insertOrUpdate(id, 0L);
        verify(historyTable, times(1)).insert(id, useAmount, TransactionType.USE, currentTimeMillis);
    }

    @Test
    @DisplayName("특정 유저의 포인트 사용이 실패합니다(사용 포인트 초과).")
    void useUserPoint_withOverPoint() {
        // given
        long id = 1L;
        long amount = 100L;

        when(userPointTable.selectById(id)).thenReturn(new UserPoint(id, amount, System.currentTimeMillis()));
        long useAmount = amount + 1L;

        // when & then
        assertThatThrownBy(() -> pointService.useUserPoint(id, useAmount)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("특정 유저의 포인트 히스토리를 조회합니다.")
    void getUserPointHistory() {
        // given
        long id = 1L;

        List<PointHistory> historyList = List.of(new PointHistory[]{
                new PointHistory(1, id, 100L, TransactionType.CHARGE, System.currentTimeMillis()),
                new PointHistory(2, id, 100L, TransactionType.USE, System.currentTimeMillis()),
        });
        when(historyTable.selectAllByUserId(id)).thenReturn(historyList);

        // when
        List<PointHistory> pointHistoryList = pointService.getUserPointHistory(id);

        // then
        assertThat(pointHistoryList).hasSize(2);

        PointHistory chargeHistory = pointHistoryList.get(0);
        assertPointHistory(chargeHistory, id, 100L, TransactionType.CHARGE);

        PointHistory useHistory = pointHistoryList.get(1);
        assertPointHistory(useHistory, id, 100L, TransactionType.USE);

        verify(historyTable, times(1)).selectAllByUserId(id);
    }

    private void assertPointHistory(PointHistory history,
                                    long expectedUserId, long expectedAmount, TransactionType expectedType) {
        assertThat(history.userId()).isEqualTo(expectedUserId);
        assertThat(history.amount()).isEqualTo(expectedAmount);
        assertThat(history.type()).isEqualTo(expectedType);
    }
}