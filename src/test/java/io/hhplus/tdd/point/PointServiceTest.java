package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
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
    @DisplayName("특정 유저의 포인트 충전을 성공한다")
    void chargeUserPoint_withValidUserIdAndPoint() {
        // given
        UserPointTable userPointTable = mock(UserPointTable.class);
        PointHistoryTable historyTable = mock(PointHistoryTable.class);
        PointService pointService = new PointService(userPointTable, historyTable);
        long id = 1L;
        long amount = 200L;
        long currentTimeMillis = System.currentTimeMillis();

        when(userPointTable.insertOrUpdate(id, amount)).thenReturn(new UserPoint(id, amount, currentTimeMillis));

        // when
        UserPoint userPoint = pointService.chargeUserPoint(id, amount);

        // then
        assertThat(userPoint.id()).isEqualTo(id);
        assertThat(userPoint.point()).isEqualTo(amount);
    }

}