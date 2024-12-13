# 재고 조회시 1,000,000 TPS 발생시 설계

## 요구사항

갤럭시 이벤트로 인해 재고 조회 API(/stock/{{vendorItemId}})의 최대 TPS가 1,000,000

## 설계 전략

 - RDS (r6g.16xlarge)
 - db.r6g.16xlarge core 개수: -  vCPU:64 memory(GiB):512 storage:EBS 전용 전용 EBS 대역폭(Mbps):19,000 네트워킹 성능(Gbps):25
 - 이 인스턴스는 단일 인스턴스로 높은 읽기 트래픽을 견딜 수 있으나, 쓰기 트래픽이 증가할 경우 성능 한계에 도달할 수 있음

## RDS의 처리 가능 트래픽

- r6g.16xlarge 기준으로, 초당 약 10,000~20,000 TPS 의 읽기 트래픽을 견딜 수 있음
- 쓰기 트래픽은 3,000~6,000 TPS 상대적으로 더 낮음

## 외부 Dependency 없이 TPS 처리 방법

 - Read Replica 사용, 쿼리 최적화 및 로컬 캐시를 통한 트래픽 분산
 - 현재는 단일서버 이므로 제한된 메모리에서 사용가능
 - 분산서버로 변경시는 application memory caching 을 하기 때문에 redis 도입이 좋움

## Amazon Aurora 도입 시

- Amazon Aurora는 읽기 복제본 자동 확장을 통해 수십만 TPS의 읽기 트래픽을 처리할 수 있으며, 쓰기 트래픽도 다중 AZ 기반으로 높은 확장성을 가짐

## Redis 도입

- Redis 도입 이유: 데이터베이스의 부하를 줄이고 TPS를 처리하기 위해 자주 조회되는 데이터를 캐싱

- 재고 데이터를 ElastiCache 로 캐싱하고, 조회 시 데이터베이스 대신 캐시된 데이터를 사용하여 TPS 요구사항을 충족

## 추후 구매가 발생할 경우의 처리

- Redis를 사용하지 않을 경우: RDS에 트랜잭션 부하가 집중되며, 쓰기 성능 병목이 발생할 가능성이 있음

- Redis 사용 시: 읽기 요청은 Redis에서 처리하고, 쓰기 요청은 RDS에서 처리하여 트래픽 분산

## 락 제어

- 락의 범위는 최대한 작게 유지 예를 들어, 레코드 단위로 잠금을 걸어 데이터베이스 성능을 극대화

## Redis Cluster 사용하여 처리
- Cluster data 샤딩을 하여 샤드가 독립적으로 데이터 관리
- Sentinel 과 장애 복구 방식은 다르게 없음
- Sentinel 사용시 단일 TPS 8,000 ~ 100,000 으로 선형적 확장이 가능한 cluster 가 대규모 트래픽에서는 더 좋은 방법임
- 최소 node 수는 Master 10 / Slave 10 으로 20개의 노드로 구성, rds 1 primary + 2 read replica 구성
- 주요 읽기 요청은 redis cluster 에서 처리하고 쓰기 및 consistency data 는 rds 로 처리

## Redis <-> RDBMS 간 데이터 inconsistency
- 비동기 업데이트 문제로 rdb 재고 업데이트 된 후, redis cache 갱신하기전 조회요청이 들어오면 old data 반환 할수 있음
- Transaction 도중에 redis 갱신 실패 || Network 문제로 데이터가 일치 하지 않을수 있음
- TTL 만료 로 인해서 Cache Miss 발생 할수 있음 만료된 시점에서 rdb 변경되엇을 경우에도 cahce data 와 rdb 데이터 불일치 발생 할수 있음
- write-through caching 전략으로 처리 rdb에 먼저 데이터가 쓰여지고 success 후 redis cache 갱신
- https://redis.io/learn/howtos/solutions/caching-architecture/write-through




