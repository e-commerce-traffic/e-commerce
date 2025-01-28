package org.project.ecommerce.common;

import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.ecommerce.common.infrastructure.consumer.StockEventConsumer;
import org.project.ecommerce.fulfillment.domain.Stock;
import org.project.ecommerce.fulfillment.domain.StockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.cassandra.core.CassandraTemplate;

import java.time.LocalDateTime;



import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
public class StockEventConsumerTest {

    @Autowired
    private StockEventConsumer consumer;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private CassandraTemplate cassandraTemplate;

    private Stock stock;
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

        // 테스트용 이벤트 페이로드
        eventPayload = """
                {
                    "skuKey": 1,
                    "finalStockCount": 90,
                    "timestamp": "%s"
                }
                """.formatted(LocalDateTime.now().toString());
    }

    @Test
    @DisplayName("최종 재고가 정상적으로 업데이트되어야 한다")
    void shouldUpdateStockToFinalStockCount() {
        // When
        consumer.handleOrderCreated(eventPayload);

        // Then
        Integer updatedStock = stockRepository.findStockCountBySkuKey(1L);
        assertThat(updatedStock).isEqualTo(90);
    }

    @Test
    @DisplayName("동일한 이벤트가 여러번 전달되어도 재고는 한번만 업데이트되어야 한다")
    void shouldBeIdempotentForSameEvent() {
        // When
        consumer.handleOrderCreated(eventPayload);
        consumer.handleOrderCreated(eventPayload);
        consumer.handleOrderCreated(eventPayload);

        // Then
        Integer finalStock = stockRepository.findStockCountBySkuKey(1L);
        assertThat(finalStock).isEqualTo(90);
    }

    @Test
    @DisplayName("이전 타임스탬프의 이벤트는 재고를 업데이트하지 않아야 한다")
    void shouldNotUpdateStockForOlderTimestamp() {
        // Given
        LocalDateTime olderTime = LocalDateTime.now().minusMinutes(1);
        LocalDateTime newerTime = LocalDateTime.now();

        String newerEvent = """
                {
                    "skuKey": 1,
                    "finalStockCount": 80,
                    "timestamp": "%s"
                }
                """.formatted(newerTime.toString());

        String olderEvent = """
                {
                    "skuKey": 1,
                    "finalStockCount": 90,
                    "timestamp": "%s"
                }
                """.formatted(olderTime.toString());

        // When
        consumer.handleOrderCreated(newerEvent); // 새로운 이벤트 먼저 처리
        consumer.handleOrderCreated(olderEvent); // 이전 이벤트 나중에 처리

        // Then
        Integer finalStock = stockRepository.findStockCountBySkuKey(1L);
        assertThat(finalStock).isEqualTo(80); // 더 최신 타임스탬프의 재고값이 유지되어야 함
    }

    @Test
    @DisplayName("더 최신 타임스탬프의 이벤트는 재고를 업데이트해야 한다")
    void shouldUpdateStockForNewerTimestamp() {
        // Given
        LocalDateTime olderTime = LocalDateTime.now().minusMinutes(1);
        LocalDateTime newerTime = LocalDateTime.now();

        String olderEvent = """
                {
                    "skuKey": 1,
                    "finalStockCount": 90,
                    "timestamp": "%s"
                }
                """.formatted(olderTime.toString());

        String newerEvent = """
                {
                    "skuKey": 1,
                    "finalStockCount": 80,
                    "timestamp": "%s"
                }
                """.formatted(newerTime.toString());

        // When
        consumer.handleOrderCreated(olderEvent); // 이전 이벤트 먼저 처리
        consumer.handleOrderCreated(newerEvent); // 새로운 이벤트 나중에 처리

        // Then
        Integer finalStock = stockRepository.findStockCountBySkuKey(1L);
        assertThat(finalStock).isEqualTo(80); // 더 최신 타임스탬프의 재고값으로 업데이트되어야 함
    }
}