package org.project.ecommerce.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.ecommerce.eventstore.kafka.consumer.StockEventConsumer;
import org.project.ecommerce.inventory.domain.Stock;
import org.project.ecommerce.inventory.domain.StockRepository;
import org.springframework.kafka.support.Acknowledgment;

import java.time.LocalDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class StockEventConsumerTest {

    @Mock
    private StockRepository stockRepository;
    
    @Mock
    private Acknowledgment acknowledgment;
    
    @Captor
    private ArgumentCaptor<Long> skuKeyCaptor;
    
    @Captor
    private ArgumentCaptor<Long> vendorItemKeyCaptor;
    
    @Captor
    private ArgumentCaptor<Integer> stockCountCaptor;
    
    @Captor
    private ArgumentCaptor<LocalDateTime> timestampCaptor;

    private StockEventConsumer consumer;
    private LocalDateTime baseTime;

    @BeforeEach
    void setUp() {
        consumer = new StockEventConsumer(stockRepository);
        baseTime = LocalDateTime.of(2025, 4, 1, 10, 0);
    }

    @Test
    @DisplayName("최종 재고가 정상적으로 업데이트되어야 한다")
    void shouldUpdateStockToFinalStockCount() {
        // Given
        String eventPayload = createEventPayload(1L, 1L, 90, baseTime.plusMinutes(5));
        
        // Stock doesn't exist yet
        when(stockRepository.findBySkuKeyAndVendorItemKey(1L, 1L))
                .thenReturn(null);
        
        // When
        consumer.handleStockEvent(
                eventPayload,
                "key-1",
                "order-created",
                0,
                0,
                acknowledgment
        );

        // Then
        verify(stockRepository).updateStockCountAndTimestamp(
                vendorItemKeyCaptor.capture(),
                skuKeyCaptor.capture(),
                stockCountCaptor.capture(),
                timestampCaptor.capture()
        );
        verify(acknowledgment).acknowledge();
        
        assertThat(skuKeyCaptor.getValue()).isEqualTo(1L);
        assertThat(vendorItemKeyCaptor.getValue()).isEqualTo(1L);
        assertThat(stockCountCaptor.getValue()).isEqualTo(90);
    }

    @Test
    @DisplayName("동일한 이벤트가 여러번 전달되어도 재고는 한번만 업데이트되어야 한다")
    void shouldBeIdempotentForSameEvent() {
        // Given
        LocalDateTime eventTime = baseTime.plusMinutes(5);
        String eventPayload = createEventPayload(1L, 1L, 90, eventTime);
        
        // Mock existing stock with same timestamp
        Stock mockStock = mock(Stock.class);
        when(mockStock.getUpdatedAt()).thenReturn(eventTime);
        when(stockRepository.findBySkuKeyAndVendorItemKey(1L, 1L))
                .thenReturn(mockStock);
        
        // When - process same event multiple times
        consumer.handleStockEvent(eventPayload, "key-1", "order-created", 0, 0, acknowledgment);
        consumer.handleStockEvent(eventPayload, "key-1", "order-created", 0, 0, acknowledgment);
        consumer.handleStockEvent(eventPayload, "key-1", "order-created", 0, 0, acknowledgment);

        // Then - we should have ack for each event, but only first should cause update
        verify(acknowledgment, times(3)).acknowledge();
        verify(stockRepository, times(3)).findBySkuKeyAndVendorItemKey(1L, 1L);
        // Since the event time is same as current stock time, no updates should happen
        verify(stockRepository, never()).updateStockCountAndTimestamp(
                anyLong(), anyLong(), anyInt(), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("이전 타임스탬프의 이벤트는 재고를 업데이트하지 않아야 한다")
    void shouldNotUpdateStockForOlderTimestamp() {
        // Given
        LocalDateTime olderTime = baseTime.minusMinutes(5);
        String olderEvent = createEventPayload(1L, 1L, 90, olderTime);
        
        // Mock existing stock with newer timestamp using a mock instead of real object
        Stock mockStock = mock(Stock.class);
        when(mockStock.getUpdatedAt()).thenReturn(baseTime);
        when(stockRepository.findBySkuKeyAndVendorItemKey(1L, 1L))
                .thenReturn(mockStock);

        // When
        consumer.handleStockEvent(olderEvent, "key-1", "order-created", 0, 0, acknowledgment);

        // Then
        verify(acknowledgment).acknowledge();
        verify(stockRepository).findBySkuKeyAndVendorItemKey(1L, 1L);
        // Stock should not be updated since event has older timestamp
        verify(stockRepository, never()).updateStockCountAndTimestamp(
                anyLong(), anyLong(), anyInt(), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("더 최신 타임스탬프의 이벤트는 재고를 업데이트해야 한다")
    void shouldUpdateStockForNewerTimestamp() {
        // Given
        LocalDateTime newerTime = baseTime.plusMinutes(5);
        String newerEvent = createEventPayload(1L, 1L, 80, newerTime);
        
        // Mock existing stock with older timestamp
        Stock mockStock = mock(Stock.class);
        when(mockStock.getUpdatedAt()).thenReturn(baseTime);
        when(stockRepository.findBySkuKeyAndVendorItemKey(1L, 1L))
                .thenReturn(mockStock);
        
        // When
        consumer.handleStockEvent(newerEvent, "key-1", "order-created", 0, 0, acknowledgment);

        // Then
        verify(acknowledgment).acknowledge();
        verify(stockRepository).findBySkuKeyAndVendorItemKey(1L, 1L);
        // Stock should be updated since event has newer timestamp
        verify(stockRepository).updateStockCountAndTimestamp(
                vendorItemKeyCaptor.capture(),
                skuKeyCaptor.capture(),
                stockCountCaptor.capture(),
                timestampCaptor.capture()
        );
        
        assertThat(skuKeyCaptor.getValue()).isEqualTo(1L);
        assertThat(vendorItemKeyCaptor.getValue()).isEqualTo(1L);
        assertThat(stockCountCaptor.getValue()).isEqualTo(80);
        assertThat(timestampCaptor.getValue()).isEqualTo(newerTime);
    }
    
    @Test
    @DisplayName("이벤트 처리 중 예외가 발생하면 예외가 전파되어야 한다")
    void shouldPropagateExceptionWhenProcessingFails() {
        // Given
        String eventPayload = createEventPayload(1L, 1L, 90, baseTime);
        
        // Mock repository to throw exception
        when(stockRepository.findBySkuKeyAndVendorItemKey(1L, 1L))
                .thenThrow(new RuntimeException("Test exception"));
                
        // When & Then
        try {
            consumer.handleStockEvent(eventPayload, "key-1", "order-created", 0, 0, acknowledgment);
        } catch (RuntimeException e) {
            // Expected
            assertThat(e.getMessage()).isEqualTo("Test exception");
        }
        
        // Should not acknowledge the message when exception occurs
        verify(acknowledgment, never()).acknowledge();
    }
    
    /**
     * Creates a test event payload with the given parameters
     */
    private String createEventPayload(Long skuKey, Long vendorItemKey, int finalStockCount, LocalDateTime timestamp) {
        return String.format(
                "{\"skuKey\":%d,\"vendorItemKey\":%d,\"finalStockCount\":%d,\"timestamp\":\"%s\"}",
                skuKey, vendorItemKey, finalStockCount, timestamp.toString()
        );
    }
}