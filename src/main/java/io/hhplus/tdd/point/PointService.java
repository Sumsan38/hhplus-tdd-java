package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static io.hhplus.tdd.point.PointPolicy.*;

@Service
@RequiredArgsConstructor
public class PointService {

    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    /**
     * ID로 특정 사용자의 포인트 정보를 검색합니다.
     * @param id 포인트 정보를 검색 할 사용자의 고유 식별자
     * @return 사용자의 포인트 데이터가 포함 된 사용자 포인트 인스턴스
     */
    public UserPoint getUserPoint(long id) {
        return userPointTable.selectById(id);
    }


    /**
     * 지정된 유저 ID와 해당 충전 금액을 사용하여 사용자의 포인트를 충전합니다.
     * 만약 금액이 허용된 최대 포인트를 초과하면 예외를 발생시킵니다.
     * 충전 후 업데이트된 사용자 포인트 데이터를 반환하며, 충전 내역은 기록됩니다.
     *
     * @param id 포인트를 충전할 사용자의 고유 식별자
     * @param amount 충전할 포인트 금액, 최대 충전 금액은 제한됩니다
     * @return 업데이트된 사용자 포인트의 데이터를 포함한 UserPoint 객체
     * @throws IllegalArgumentException 입력된 충전 금액이 최대 포인트 한도를 초과하는 경우
     */
    public UserPoint chargeUserPoint(long id, long amount) {
        UserPoint userPoint = userPointTable.selectById(id);
        long updatedPoint = userPoint.point() + amount;
        if(amount > MAX_POINT || updatedPoint > MAX_POINT) {
            throw new IllegalArgumentException("유저의 포인트 초과입니다.");
        }

        UserPoint updateUserPoint = userPointTable.insertOrUpdate(id, updatedPoint);
        pointHistoryTable.insert(id, amount, TransactionType.CHARGE, updateUserPoint.updateMillis());

        return updateUserPoint;
    }
}