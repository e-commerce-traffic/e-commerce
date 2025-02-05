# 카프카 파티션 증설 시 발생 문제 및 대처 방안

## 1. 개요

- 트래픽 증가로 TPS 발행량이 급증할 때, **카프카 토픽의 파티션**을 을려 **처리량**을 높이고자 함.

## 2. 발생 가능한 문제

1. **Consumer Rebalance 비용 증가**
    - 파티션 수를 늘리면 Consumer Group 이 **Rebalance** 를 수행
    - **메시지 처리 공백** 이 생기거나, 일부 컨슈머가 과도한 부하를 받는 문제가 발생
    - 새로운 파티션이 생김 -> 기존 컨슈머들에 미할당 상태 -> 기존 파티션 할당을 그대로 유지하면 새로운 파티션을 소비할 컨슈머가 없음
    - 카프카가 모든 파티션을 다시 컨슈머들에게 할당하기 위해 Rebalance 수행

2. **순서 보장 이슈**
    - 기존에는 같은 키가 동일 파티션에 매핑되어 이벤트 순서가 보장되었음
    - 파티션 확장 후, 해시 전략이 달라지면서 같은 키의 이벤트가 다른 파티션에 들어가 순서 보장이 안됨

3. **데이터 불균형**
    - 파티션을 단순히 늘렸다고 해서 메시지가 고르게 분산되는것은 아님.
    - 해시 파티셔닝 방식에 따라 특정 파티션에 키가 몰리는 핫 스팟 문제가 생길 수 있음

4. **Replication 부하 증가
    - 파티션이 많아지면 브로커 간 ISR 복제 오버헤드 증가
    - 잘못 늘리면 오히려 쓰기 지연과 replication log 가 커질 수 있음

## 3. 해결 방안

1. **안정해시** 기반 파티셔닝
    - application level 에서 안정해싱 을 구현 하여 파티션 수가 늘어나도 동일 키는 항상 같은 파티션으로 매핑되도록 함
    - 데이터 일관성이 중요할때 추천
2. **파티션을 늘리지 않고 처리**
    - 배치 전송과 압축을 해서 카프카 쓰기 성능 5~10배 항샹 가능
    - consumer 병렬 처리를 통한 멀티 스레드 활용하여 컨슈머 개수를 늘리면 5~10배 향상 가능

# 최대 500만 TPS(쓰기) 대응

## 1. 개용

- **시나리오**: 주문 트래픽(쓰기)이 최대 500만 TPS에 달할 수 있음
- **목표**:
    - 높은 트래픽에도 수평 확장이 가능한 **AP(가용성 우선)** 구조
    - **실시간 재고 확인**을 지향하되, 네트워크 파티션 등에서의 **최종 일관성**을 보장

## 2. 목표와 전제

1. **주문(쓰기) 트래픽 5백만 TPS**까지 확장 가능해야 함.
2. **AP 성격**을 우선으로 선택 (네트워크 이슈 시에도 서비스 중단 없이 동작).
3. **실시간 재고 확인**을 지향
    - 주문 시점에 Cassandra에 **즉시** 반영한 후, 조회 시점에도 Cassandra **(Quorum Read 등)** 로 접근
    - 일반적으로 수 밀리초 ~ 수십 밀리초 단위의 내부 복제로 인해, 사용자 체감상 실시간에 가깝게 재고 데이터 확인 가능
    - 완벽한 동시성 제어(강 일관성)는 어려우나, “최종 일관성”을 통해 가용성과 스케일을 극대화

## 3. 해결 방안

### 3.1 Event Sourcing 패턴 (선택 사항)

- **주문 시** `OrderRequestDto`를 받아 **카프카**(`order-created` 토픽 등)에 메시지 전송 → 비동기로 Cassandra 저장
- 주문 단계에서 `order-created` → `order-validated` → `order-confirmed` 등의 이벤트로 상태를 업데이트
- **카프카 스트림**을 통해 재고 변동 이벤트를 실시간으로 처리
    - 예) `stock_state` 테이블: 현재 재고 상태 (AP+실시간 조회용)
    - 예) `stock_update` 테이블: 재고 변경 이력 (이벤트 소싱·로그용)
    - 단일 테이블만으로도 충분하지만, 이벤트 소싱/로그가 필요하다면 분리 운영

### 3.2 주문 시점의 원자적 재고 감소

- 주문 API를 받으면 **Cassandra**에 “(원자적) 재고 차감 or 예약”을 시도
    - **성공** → 재고 충분
    - **실패** → 재고 부족 → 즉시 에러 반환
- Cassandra는 **수평 확장**으로 5백만 TPS 쓰기 부하에 대응 가능
- “원자적 감소” 구현
    - (옵션 A) **Lightweight Transaction(LWT)** 사용: 정확도가 높으나 TPS 손실 가능
    - (옵션 B) **Counter** 기반 증감: 높은 성능, 단 oversell 가능성 → 후처리 보정
- **RDB**에는 주문 기록 등 최소 정보만 저장(트랜잭션 보장, Outbox 등).
    - 카프카로 이벤트 발행 → Cassandra 컨슈머가 보조 작업(로그 축적, 모니터링) 수행

## 4. 시퀀스 다이어그램

```mermaid
sequenceDiagram
    autonumber
    participant Client as Client
    participant OrderService as OrderService
    participant Cassandra as Cassandra (NoSQL)
    participant RDB as RDB (Orders+Outbox)
    participant Kafka as Kafka Cluster
    participant StockConsumer as StockEventConsumer

    Client->>OrderService: 1) 주문 요청 (최대 500만 TPS)
    Note over OrderService: 2) Cassandra 재고 차감 시도
    
    OrderService->>Cassandra: UPDATE stock (원자적 감소 or 예약)
    Cassandra-->>OrderService: 재고 충분 / 재고 부족

    alt 재고 충분
        Note over OrderService: 3) RDB에 주문 정보 INSERT
        OrderService->>RDB: INSERT orders(...)
        RDB-->>OrderService: Commit
        Note over OrderService: 4) Outbox 이벤트 INSERT 후 Commit
        OrderService->>RDB: INSERT outbox_event
        RDB-->>OrderService: Commit
        OrderService->>Kafka: Produce "order-created"
        Kafka-->>StockConsumer: 이벤트 소비
        Note over StockConsumer: Cassandra 최종 재고상태/이력 업데이트
    else 재고 부족
        OrderService-->>Client: 400 OutOfStock
    end