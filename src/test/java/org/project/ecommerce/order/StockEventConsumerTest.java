package org.project.ecommerce.order;

import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.ecommerce.common.infrastructure.consumer.StockEventConsumer;
import org.project.ecommerce.common.infrastructure.outbox.OutBoxEventRepository;
import org.project.ecommerce.common.infrastructure.outbox.OutboxEvent;
import org.project.ecommerce.fulfillment.domain.Stock;
import org.project.ecommerce.fulfillment.domain.StockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.cassandra.core.CassandraTemplate;

import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
public class StockEventConsumerTest {

    @Autowired
    private StockEventConsumer consumer;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private OutBoxEventRepository outboxRepository;

    @Autowired
    private CassandraTemplate cassandraTemplate;

    private Stock stock;
    private OutboxEvent outboxEvent;
    private String eventPayload;


    @BeforeEach
    void setUp() {
        // 테이블 초기화
        SimpleStatement statement = SimpleStatement.builder("TRUNCATE stocks").build();
        cassandraTemplate.execute(statement);

        // 재고 데이터 설정
        stock = new Stock();
        stock.setSkuKey(1L);
        stock.setStockCount(100);
        stock.setUpdatedAt(LocalDateTime.now());
        cassandraTemplate.insert(stock);


        // Outbox 이벤트 생성 및 저장 (상태를 PENDING으로 설정)
        this.outboxEvent = OutboxEvent.createEvent("OrderCreated",  // this.outboxEvent에 할당
                "{\"skuKey\": 1, \"decreasedCount\": 10}");
        this.outboxEvent = outboxRepository.save(this.outboxEvent);  // 저장된 엔티티를 다시 할당

        // 테스트용 이벤트 페이로드
        eventPayload = """
                {
                    "skuKey": 1,
                    "decreasedCount": 10,
                    "outboxId": %d
                }
                """.formatted(outboxEvent.getId());
    }

    @Test
    @DisplayName("재고가 충분하면 재고가 정상적으로 차감되어야 한다")
    void shouldDecreaseStockWhenSufficientStock() {
        // When
        consumer.handleOrderCreated(eventPayload);

        // Then
        Integer updatedStock = stockRepository.findStockCountBySkuKey(1L);
        assertThat(updatedStock).isEqualTo(90);

        // Outbox 이벤트 상태 확인
        OutboxEvent processedEvent = outboxRepository.findById(outboxEvent.getId()).orElseThrow();
        assertThat(processedEvent.getStatus()).isEqualTo(OutboxEvent.OutboxStatus.COMPLETED);
    }

    @Test
    @DisplayName("재고가 부족하면 재고가 차감되지 않아야 한다")
    void shouldNotDecreaseStockWhenInsufficientStock() {
        // Given
        OutboxEvent largeEvent = OutboxEvent.createEvent("OrderCreated",
                "{\"skuKey\": 1, \"decreasedCount\": 150}");
        largeEvent = outboxRepository.save(largeEvent);

        String eventWithLargeCount = """
            {
                "skuKey": 1,
                "decreasedCount": 150,
                "outboxId": %d
            }
            """.formatted(largeEvent.getId());

        // When & Then
        assertThrows(IllegalStateException.class, () ->
                consumer.handleOrderCreated(eventWithLargeCount));

        // 재고 확인
        Integer currentStock = stockRepository.findStockCountBySkuKey(1L);
        assertThat(currentStock).isEqualTo(100);

        // 에러 발생 시 OrderEventHandler에서 상태를 FAILED로 변경하므로,
        // 여기서는 상태 검증을 제거하거나 PENDING 상태를 검증
        OutboxEvent event = outboxRepository.findById(largeEvent.getId()).orElseThrow();
//        assertThat(event.getStatus()).isEqualTo(OutboxEvent.OutboxStatus.PENDING);
        assertThat(event.getStatus()).isEqualTo(OutboxEvent.OutboxStatus.FAILED);
//        assertThat(event.getPublishedAt()).isNotNull();
    }

    @Test
    @DisplayName("동일한 이벤트가 여러번 전달되어도 재고는 한번만 차감되어야 한다")
    void shouldBeIdempotentForSameEvent() {
        // When
        consumer.handleOrderCreated(eventPayload);
        consumer.handleOrderCreated(eventPayload);
        consumer.handleOrderCreated(eventPayload);

        // Then
        Integer finalStock = stockRepository.findStockCountBySkuKey(1L);
        assertThat(finalStock).isEqualTo(90);

        // Outbox 이벤트 상태 확인
        OutboxEvent processedEvent = outboxRepository.findById(outboxEvent.getId()).orElseThrow();
        assertThat(processedEvent.getStatus()).isEqualTo(OutboxEvent.OutboxStatus.COMPLETED);
    }

//    @Test
//    @DisplayName("여러 요청이 동시에 들어와도 재고 정합성이 유지되어야 한다")
//    void shouldMaintainStockConsistencyUnderConcurrency() throws InterruptedException {
//        int threadCount = 10;
//        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
//        CountDownLatch latch = new CountDownLatch(threadCount);
//
//        // When
//        for (int i = 0; i < threadCount; i++) {
//            executorService.submit(() -> {
//                try {
//                    consumer.handleOrderCreated(eventPayload);
//                } finally {
//                    latch.countDown();
//                }
//            });
//        }
//
//        latch.await(5, TimeUnit.SECONDS);
//
//        // Then
//        Integer finalStock = stockRepository.findStockCountBySkuKey(1L);
//        assertThat(finalStock).isEqualTo(90);
//
//        // Outbox 이벤트 상태 확인
//        OutboxEvent processedEvent = outboxRepository.findById(outboxEvent.getId()).orElseThrow();
//        assertThat(processedEvent.getStatus()).isEqualTo(OutboxEvent.OutboxStatus.COMPLETED);
//
//        executorService.shutdown();
//    }


    @Test
    @DisplayName("여러 요청이 동시에 들어와도 재고 정합성이 유지되어야 한다")
    void shouldMaintainStockConsistencyUnderConcurrency() throws InterruptedException {
        // Given
        List<OutboxEvent> events = new ArrayList<>();
        List<String> payloads = new ArrayList<>();

        // 각각 다른 outbox 이벤트 생성
        for (int i = 0; i < 10; i++) {
            OutboxEvent event = OutboxEvent.createEvent("OrderCreated",
                    "{\"skuKey\": 1, \"decreasedCount\": 10}");
            events.add(outboxRepository.save(event));

            payloads.add("""
            {
                "skuKey": 1,
                "decreasedCount": 10,
                "outboxId": %d
            }
            """.formatted(event.getId()));
        }

        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // When - 각각 다른 이벤트로 재고 차감 시도
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    consumer.handleOrderCreated(payloads.get(index));
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(5, TimeUnit.SECONDS);

        // Then
        // 재고는 하나의 요청만 성공해야 함
        Integer finalStock = stockRepository.findStockCountBySkuKey(1L);
        assertThat(finalStock).isEqualTo(90);

        // 이벤트 상태 확인 - 하나는 COMPLETED, 나머지는 FAILED
        long completedCount = events.stream()
                .map(e -> outboxRepository.findById(e.getId()).orElseThrow())
                .filter(e -> e.getStatus() == OutboxEvent.OutboxStatus.COMPLETED)
                .count();
        assertThat(completedCount).isEqualTo(1);
    }
}