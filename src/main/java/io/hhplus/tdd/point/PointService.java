package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import static io.hhplus.tdd.point.PointPolicy.MAX_POINT;

@Service
@RequiredArgsConstructor
public class PointService {

    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    // 사용자별 락 저장소
    private static final ConcurrentHashMap<Long, ReentrantLock> userLockMap = new ConcurrentHashMap<>();

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
        ReentrantLock lock = userLockMap.computeIfAbsent(id, l -> new ReentrantLock());
        lock.lock();

        try {
            UserPoint userPoint = userPointTable.selectById(id);
            long updatedPoint = userPoint.point() + amount;
            if(amount > MAX_POINT || updatedPoint > MAX_POINT) {
                throw new IllegalArgumentException("유저의 포인트 초과입니다.");
            }

            UserPoint updateUserPoint = userPointTable.insertOrUpdate(id, updatedPoint);
            pointHistoryTable.insert(id, amount, TransactionType.CHARGE, updateUserPoint.updateMillis());

            return updateUserPoint;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 사용자의 포인트를 지정된 금액만큼 차감합니다.
     * 포인트 차감 후 업데이트된 사용자 포인트 데이터를 반환하며, 차감 기록이 저장됩니다.
     *
     * @param id 포인트를 차감할 사용자의 고유 식별자
     * @param amount 차감할 포인트 금액
     * @return 업데이트된 사용자 포인트의 데이터를 포함한 UserPoint 객체
     * @throws IllegalArgumentException 입력된 차감 금액이 사용자의 보유 포인트를 초과하는 경우
     */
    public UserPoint useUserPoint(long id, long amount) {
        ReentrantLock lock = userLockMap.computeIfAbsent(id, l -> new ReentrantLock());
        lock.lock();

        try {
            UserPoint userPoint = userPointTable.selectById(id);
            long updatedPoint = userPoint.point() - amount;
            if(updatedPoint < 0) {
                throw new IllegalArgumentException("유저의 포인트가 부족합니다.");
            }

            UserPoint updateUserPoint = userPointTable.insertOrUpdate(id, updatedPoint);
            pointHistoryTable.insert(id, amount, TransactionType.USE, updateUserPoint.updateMillis());

            return updateUserPoint;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 제공된 사용자 ID를 기반으로 특정 사용자의 포인트 히스토리를 검색합니다.
     *
     * @param id 포인트 이력을 검색 할 사용자의 고유 식별자입니다.
     * @return 사용자의 포인트 pointhistory 객체 목록
     */
    public List<PointHistory> getUserPointHistory(long id) {
        return pointHistoryTable.selectAllByUserId(id);
    }
}