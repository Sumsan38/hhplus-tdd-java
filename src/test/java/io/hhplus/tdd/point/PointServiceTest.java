package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.hhplus.tdd.point.PointPolicy.MAX_POINT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class PointServiceTest {

    @Test
    @DisplayName("특정 유저의 포인트를 조회합니다.")
    void getUserPoint_ReturnValidId() {
        // given
        UserPointTable userPointTable = mock(UserPointTable.class);
        PointHistoryTable historyTable = mock(PointHistoryTable.class);
        PointService pointService = new PointService(userPointTable, historyTable);
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
    void chargeUserPoint_withValidUserIdAndPoint_And() {
        // given
        UserPointTable userPointTable = mock(UserPointTable.class);
        PointHistoryTable historyTable = mock(PointHistoryTable.class);
        PointService pointService = new PointService(userPointTable, historyTable);
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

        verify(userPointTable, times(1)).insertOrUpdate(id, updatedPoint);
        verify(historyTable, times(1)).insert(id, amount, TransactionType.CHARGE, currentTimeMillis);
    }
    
    @Test
    @DisplayName("특정 유저의 포인트 충전이 실패합니다(최대 잔고 초과).")
    void chargeUserPoint_withOverPoint() {
        // given
        UserPointTable userPointTable = mock(UserPointTable.class);
        PointHistoryTable historyTable = mock(PointHistoryTable.class);
        PointService pointService = new PointService(userPointTable, historyTable);
        long id = 1L;
        long beforePoint = 1L;
        long amount = MAX_POINT;
        long currentTimeMillis = System.currentTimeMillis();

        when(userPointTable.selectById(id)).thenReturn(new UserPoint(id, beforePoint, currentTimeMillis));

        // when & then
        assertThrows(IllegalArgumentException.class, () -> pointService.chargeUserPoint(id, amount));
        verify(historyTable, never()).insert(id, amount, TransactionType.CHARGE, currentTimeMillis);
    }

}