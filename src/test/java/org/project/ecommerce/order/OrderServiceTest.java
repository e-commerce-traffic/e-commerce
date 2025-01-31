package org.project.ecommerce.order;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.ecommerce.common.infrastructure.outbox.OutBoxEventRepository;
import org.project.ecommerce.common.infrastructure.outbox.OutboxEvent;
import org.project.ecommerce.common.infrastructure.utils.JsonUtils;
import org.project.ecommerce.fulfillment.application.StockService;
import org.project.ecommerce.order.application.OrderService;
import org.project.ecommerce.order.domain.OrderRepository;
import org.project.ecommerce.order.ui.dto.OrderItemDto;
import org.project.ecommerce.order.ui.dto.OrderRequestDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@SpringBootTest
class OrderServiceTest {
    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OutBoxEventRepository outboxRepository;

    @MockitoSpyBean
    private StockService stockService;


    @BeforeEach
    void setUp() {
        // 테스트 전 데이터 초기화
        orderRepository.deleteAll();
        outboxRepository.deleteAll();
    }

    private OrderRequestDto createSampleOrderRequest() {
        OrderItemDto itemDto = new OrderItemDto(1L, 10); // vendorItemId, count
        return new OrderRequestDto(1L, List.of(itemDto));  // userId, orderItems
    }

    @Test
    @DisplayName("주문이 성공적으로 생성되어야 한다")
    void shouldCreateOrder() {
        // Given
        String idempotencyKey = UUID.randomUUID().toString();
        OrderRequestDto dto = createSampleOrderRequest();

        // Mock 설정 구체화
        when(stockService.getStock(anyList())).thenReturn(100);

        // When
        orderService.createOrder(dto, idempotencyKey);

        // Then
        assertTrue(orderRepository.existsByIdempotencyKey(idempotencyKey));

        // Outbox 이벤트 검증 - Enum.name() 사용
        List<OutboxEvent> events = outboxRepository.findByStatus(OutboxEvent.OutboxStatus.COMPLETED);
        assertFalse(events.isEmpty());

        OutboxEvent event = events.get(0);
        assertThat(event.getEventType()).isEqualTo("OrderCreated");

        JsonNode payload = JsonUtils.parse(event.getPayload());
        assertThat(payload.get("skuKey")).isNotNull();
        assertThat(payload.get("finalStockCount")).isNotNull();
        assertThat(payload.get("timestamp")).isNotNull();
    }

    @Test
    @DisplayName("동일한 멱등성 키로 중복 주문이 불가능해야 한다")
    void shouldPreventDuplicateOrder() {
        // Given
        String idempotencyKey = UUID.randomUUID().toString();
        OrderRequestDto dto = createSampleOrderRequest();
        when(stockService.getStock(anyList())).thenReturn(100);

        // When
        orderService.createOrder(dto, idempotencyKey);

        // Then
        // 1. 동일한 키로 재주문 시 예외 발생 확인
        assertThrows(IllegalStateException.class, () ->
                orderService.createOrder(dto, idempotencyKey)
        );

        // 2. stockService가 한 번만 호출되었는지 확인
        verify(stockService, times(1)).getStock(anyList());

        // 3. 첫 주문에 대한 Outbox 이벤트만 생성되었는지 확인
        List<OutboxEvent> events = outboxRepository.findAll();
        assertEquals(1, events.size());
    }

    @Test
    @DisplayName("재고가 부족하면 주문이 실패해야 한다")
    void shouldFailOrderWhenInsufficientStock() {
        // Given
        String idempotencyKey = UUID.randomUUID().toString();
        OrderRequestDto dto = createSampleOrderRequest();
        doReturn(0).when(stockService).getStock(any());

        // When & Then
        assertThrows(IllegalStateException.class, () ->
                orderService.createOrder(dto, idempotencyKey)
        );
    }


    @Test
    @DisplayName("주문 생성 시 OutboxEvent가 생성되어야 한다")
    void shouldCreateOutboxEventWhenOrderCreated() {
        // Given
        String idempotencyKey = UUID.randomUUID().toString();
        OrderRequestDto dto = createSampleOrderRequest();
        doReturn(100).when(stockService).getStock(any());

        // When
        orderService.createOrder(dto, idempotencyKey);

        // Then
        List<OutboxEvent> events = outboxRepository.findByStatus(OutboxEvent.OutboxStatus.PENDING);
        assertThat(events).hasSize(dto.getOrderItem().size());
    }
}