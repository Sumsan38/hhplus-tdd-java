# 동시성 제어 방식에 대한 분석 및 보고서

---
## 개요
이번 프로젝트는 포인트의 충전, 사용 시스템을 개발합니다. <br/>
동시에 여러 겅의 포인트 충전, 이용 요청이 들어올 경우 순차적으로 처리되어야합니다. <br/>
분산 환경은 고려하지 않습니다. <br/>

동시성 제어의 경우 사용자별 id를 키 값으로, 포인트의 충전과 사용과 관련된 코드에 ```ReentrantLock```을 가지고 있는 ```ConcurrentHashMap```을 사용합니다.

```java
// 사용자별 락 저장소
ConcurrentHashMap<Long, ReentrantLock> userLockMap = new ConcurrentHashMap<>();

ReentrantLock lock = userLockMap.computeIfAbsent(id, l -> new ReentrantLock());
lock.lock();

try {
    // 충전 및 사용 로직
} finally {
    lock.unlock();
}
```
---

## 테스트 케이스
1. ```chargeUserPoint_concurrency``` 동시에 여러 충전 요청이 들어올 경우에도 포인트 충전은 정확히 반영된다.
2. ```useUserPoint_concurrency``` 동시에 여러 요청이 들어와도 포인트 사용은 모두 반영된다.
3. ```useUserPoint_concurrency_withInsufficientPoint``` 잔액이 부족할 때 포인트 사용 요청이 들어올 경우 실패하는 경우도 존재한다.
---

## 사용 기술
### 1. ReentrantLock
- 하나의 Thread가 여러번 반복해서 lock을 휙득할 수 있게 해주는 동기화 도구입니다.

기존에는 ```synchronized```키워드로도 락을 걸 수 있으나, 다음과 같은 단점이 있습니다.
```java
synchronized (lockObject) {
    // 락이 걸린 코드
}
```
1. timeout 설정 불가
2. lock 휙득 성공 여부 확인 불가
3. 공정성(fairness: 먼저 기다린 Thread에게 lock을 먼저 부여한다) 설정 불가

이러한 단점들을 보완한 것이 ```ReetrantLock```입니다.

```java
ReentrantLock lock = new ReentrantLock();

lock.lock(); // lock 획득
try {
    // 임계 구역
    // 여러 스레드가 동시에 접근하면 안 되는 부분
} finally {
    lock.unlock(); // lock 해제. 반드시 해제해야 데드락 방지
}
```
해당 프로젝트에서는 비공정 lock을 사용했습니다.

### 2. ExecutorService
- 자바에서 Thread를 직접 만들지 않고 작업을 병렬호 실행할 수 있게 해주는 Thread Pool 관리 객체입니다.
- 최대 n개의 Thread로 작업을 동시에 실행합니다.
- 내부적으로 Thread를 재사용해서 성능을 최적화합니다.
```java
// n개의 Thred pool을 관리하는 객체입니다.
ExecutorService executor = Executors.newFixedThreadPool(n);
```

### 3. CompletableFuture
- 비동기 작업을 실행하는 객체입니다.
- 작업을 **Thread Pool에 전달해서 실행**하고, 결과는 기다리지 않습니다.
- 지정된 Runable 작업을 비동기로 실행합니다.
- 특정 ExecutorService를 지정하면 원하는 Thread Pool에서 병렬 실행이 가능합니다. 
```java
// n개의 Thred pool을 관리하는 객체입니다.
ExecutorService executor = Executors.newFixedThreadPool(n);

CompletableFuture.runAsync(() -> {
    // Runable 작업 (비동기로 실행)
}, executor);
```
위 코드는 비동기로 실행되는 작업을 executor에 등록해서 비동기로 실행합니다. 

### 4. CountDownLatch
- 여러 Thread 작업이 끝날때 까지 기다리기 위해 사용되는 동기화 도구입니다.
- 초기 숫자를 ```N```으로 설정합니다. ```new CountDownLatch(N)```
- 각 Thread 작업이 끝날때 ```countDown()```을 호출합니다. 이를 통해 내부 카운트가 감소합니다.
- 메인 Thread는 ```await()```을 호출하여 모든 작업이 끝날 때 까지 blocking 합니다.
```java
// 100개의 latch를 설정합니다.
CountDownLatch latch = new CountDownLatch(100);

for (int i = 0; i < 100; i++) {
    CompletableFuture.runAsync(() -> {
        try {
            // Runable 작업 (비동기로 실행)
        } finally {
            latch.countDown(); // 한 작업 종료
        }
    }, executor);
}

latch.await(); // 100개의 작업이 끝날 때까지 대기
```
