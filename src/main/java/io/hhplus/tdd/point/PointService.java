package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PointService {

    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    private static final Long MAX_POINT = 100_000_000L;

    /**
     * ID로 특정 사용자의 포인트 정보를 검색합니다.
     * @param id 포인트 정보를 검색 할 사용자의 고유 식별자
     * @return 사용자의 포인트 데이터가 포함 된 사용자 포인트 인스턴스
     */
    public UserPoint getUserPoint(long id) {
        return userPointTable.selectById(id);
    }


    public UserPoint chargeUserPoint(long id, long amount) {
        UserPoint userPoint = userPointTable.selectById(id);


        return null;
    }
}
