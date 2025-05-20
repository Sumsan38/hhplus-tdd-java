package io.hhplus.tdd.point;

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
        PointService pointService = new PointService(userPointTable);
        long id = 1L;

        when(userPointTable.selectById(id)).thenReturn(new UserPoint(id, 100, 1000L));
        
        // when
        UserPoint userPoint = pointService.getUserPoint(id);

        // then
        assertThat(userPoint.id()).isEqualTo(id);
        verify(userPointTable, times(1)).selectById(id);
    }

}