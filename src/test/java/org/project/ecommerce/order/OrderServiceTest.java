package org.project.ecommerce.order;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.ecommerce.common.infrastructure.outbox.EventType;
import org.project.ecommerce.common.infrastructure.outbox.OutBoxEventRepository;
import org.project.ecommerce.common.infrastructure.outbox.OutboxEvent;
import org.project.ecommerce.common.infrastructure.outbox.OutboxEventCreator;
import org.project.ecommerce.common.infrastructure.utils.JsonUtils;
import org.project.ecommerce.fulfillment.application.StockService;
import org.project.ecommerce.fulfillment.domain.Sku;
import org.project.ecommerce.fulfillment.domain.SkuRepository;
import org.project.ecommerce.fulfillment.domain.VendorItemSku;
import org.project.ecommerce.fulfillment.domain.VendorItemSkuRepository;
import org.project.ecommerce.order.application.OrderService;
import org.project.ecommerce.order.domain.Order;
import org.project.ecommerce.order.domain.OrderRepository;
import org.project.ecommerce.order.domain.VendorItem;
import org.project.ecommerce.order.domain.VendorItemRepository;
import org.project.ecommerce.order.ui.dto.OrderItemDto;
import org.project.ecommerce.order.ui.dto.OrderRequestDto;
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    
    @Mock
    private OrderRepository orderRepository;
    
    @Mock
    private OutBoxEventRepository outboxRepository;
    
    @Mock
    private SkuRepository skuRepository;
    
    @Mock
    private StockService stockService;
    
    @Mock
    private VendorItemRepository vendorItemRepository;
    
    @Mock
    private VendorItemSkuRepository vendorItemSkuRepository;
    
    @Mock
    private OutboxEventCreator eventCreator;
    
    @Mock
    private ApplicationEventPublisher eventPublisher;
    
    @Captor
    private ArgumentCaptor<Order> orderCaptor;
    
    @Captor
    private ArgumentCaptor<Sku> skuCaptor;
    
    @Captor
    private ArgumentCaptor<OutboxEvent> outboxEventCaptor;
    
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(
            vendorItemRepository,
            skuRepository,
            null, // inboundRepository - not used in our tests
            orderRepository,
            vendorItemSkuRepository,
            outboxRepository,
            eventCreator,
            stockService,
            eventPublisher
        );
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
        
        // Setup test data - using proper mocks instead of real objects with setters
        VendorItem vendorItem = VendorItem.builder()
            .id(1L)
            .name("Test Product")
            .build();
            
        // Sku 객체를 mock으로 생성하여 setter 사용 회피
        Sku sku = mock(Sku.class);
        when(sku.getId()).thenReturn(1L);
        when(sku.getStockCount()).thenReturn(100, 90); // 처음에는 100, 감소 후에는 90 반환
        
        VendorItemSku vendorItemSku = VendorItemSku.builder()
            .vendorItem(vendorItem)
            .sku(sku)
            .build();

        List<VendorItemSku> vendorItemSkus = List.of(vendorItemSku);
        when(vendorItem.getVendorItemSkus()).thenReturn(vendorItemSkus);
        
        // Mock repository responses
        when(orderRepository.existsByIdempotencyKey(idempotencyKey)).thenReturn(false);
        when(vendorItemRepository.findById(dto.getOrderItem().get(0).getVendorItemId()))
            .thenReturn(Optional.of(vendorItem));
        when(stockService.getStockByVendorItemId(1L)).thenReturn(100);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            if (order.getId() == null) {
                // Create a new Order with the same properties but with an ID
                // This simulates what JPA would do
                Order newOrder = Order.create(order.getUserId(), order.getIdempotencyKey());
                // Use reflection to set the ID since we can't directly modify the immutable object
                try {
                    java.lang.reflect.Field idField = Order.class.getDeclaredField("id");
                    idField.setAccessible(true);
                    idField.set(newOrder, 1L);
                    return newOrder;
                } catch (Exception e) {
                    return order; // fallback
                }
            }
            return order;
        });
        
        // Mock outbox event creation
        OutboxEvent mockEvent = OutboxEvent.createEvent(
            EventType.ORDER_CREATED.name(), 
            "{\"skuKey\":1,\"finalStockCount\":90,\"timestamp\":\"2025-04-02T10:00:00\"}"
        );
        when(eventCreator.createStockEvents(any(), anyLong(), anyInt(), anyLong()))
            .thenReturn(List.of(mockEvent));

        // When
        orderService.createOrder(dto, idempotencyKey);

        // Then
        verify(orderRepository).existsByIdempotencyKey(idempotencyKey);
        verify(orderRepository, times(2)).save(orderCaptor.capture());
        verify(skuRepository).save(skuCaptor.capture());
        verify(eventCreator).createStockEvents(eq(EventType.ORDER_CREATED), eq(1L), eq(90), eq(1L));
        verify(eventPublisher).publishEvent(any(OutboxEvent.class));
        
        // Mock Sku이므로 decreaseStock 메서드가 호출되었는지만 검증
        verify(sku).decreaseStock(10);
        
        // Validate order is correctly created
        List<Order> capturedOrders = orderCaptor.getAllValues();
        Order finalOrder = capturedOrders.get(capturedOrders.size() - 1);
        assertNotNull(finalOrder);
        assertEquals(idempotencyKey, finalOrder.getIdempotencyKey());
    }

    @Test
    @DisplayName("동일한 멱등성 키로 중복 주문이 불가능해야 한다")
    void shouldPreventDuplicateOrder() {
        // Given
        String idempotencyKey = UUID.randomUUID().toString();
        OrderRequestDto dto = createSampleOrderRequest();
        
        // Mock repository to simulate existing order with same idempotency key
        when(orderRepository.existsByIdempotencyKey(idempotencyKey)).thenReturn(true);

        // When & Then
        assertThrows(IllegalStateException.class, () ->
                orderService.createOrder(dto, idempotencyKey)
        );
        
        // Verify we called existsByIdempotencyKey but didn't proceed further
        verify(orderRepository).existsByIdempotencyKey(idempotencyKey);
        verify(stockService, never()).getStockByVendorItemId(anyLong());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("재고가 부족하면 주문이 실패해야 한다")
    void shouldFailOrderWhenInsufficientStock() {
        // Given
        String idempotencyKey = UUID.randomUUID().toString();
        OrderRequestDto dto = createSampleOrderRequest();
        
        // Setup test data
        VendorItem vendorItem = VendorItem.builder()
            .id(1L)
            .name("Test Product")
            .build();
        
        // Mock insufficient stock
        when(orderRepository.existsByIdempotencyKey(idempotencyKey)).thenReturn(false);
        when(vendorItemRepository.findById(dto.getOrderItem().get(0).getVendorItemId()))
            .thenReturn(Optional.of(vendorItem));
        when(stockService.getStockByVendorItemId(1L)).thenReturn(5); // Less than requested 10
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            if (order.getId() == null) {
                // Use reflection to set ID
                try {
                    java.lang.reflect.Field idField = Order.class.getDeclaredField("id");
                    idField.setAccessible(true);
                    idField.set(order, 1L);
                } catch (Exception e) {
                    // Ignore
                }
            }
            return order;
        });

        // When & Then
        assertThrows(IllegalStateException.class, () ->
                orderService.createOrder(dto, idempotencyKey)
        );
        
        // Verify we checked the stock but didn't save the sku
        verify(stockService).getStockByVendorItemId(1L);
        verify(skuRepository, never()).save(any(Sku.class));
    }

    @Test
    @DisplayName("주문 생성 시 이벤트가 발행되어야 한다")
    void shouldPublishEventsWhenOrderCreated() {
        // Given
        String idempotencyKey = UUID.randomUUID().toString();
        OrderRequestDto dto = createSampleOrderRequest();
        
        // Setup test data
        VendorItem vendorItem = VendorItem.builder()
            .id(1L)
            .name("Test Product")
            .build();
            
        // Sku 객체를 mock으로 생성하여 setter 사용 회피
        Sku sku = mock(Sku.class);
        when(sku.getId()).thenReturn(1L);
        when(sku.getStockCount()).thenReturn(100, 90); // 처음에는 100, 감소 후에는 90 반환
        
        VendorItemSku vendorItemSku = VendorItemSku.builder()
            .vendorItem(vendorItem)
            .sku(sku)
            .build();

        List<VendorItemSku> vendorItemSkus = List.of(vendorItemSku);
        when(vendorItem.getVendorItemSkus()).thenReturn(vendorItemSkus);
        
        // Mock repository responses
        when(orderRepository.existsByIdempotencyKey(idempotencyKey)).thenReturn(false);
        when(vendorItemRepository.findById(dto.getOrderItem().get(0).getVendorItemId()))
            .thenReturn(Optional.of(vendorItem));
        when(stockService.getStockByVendorItemId(1L)).thenReturn(100);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            if (order.getId() == null) {
                // Use reflection to set ID
                try {
                    java.lang.reflect.Field idField = Order.class.getDeclaredField("id");
                    idField.setAccessible(true);
                    idField.set(order, 1L);
                } catch (Exception e) {
                    // Ignore
                }
            }
            return order;
        });
        
        // Mock outbox event creation
        OutboxEvent mockEvent = OutboxEvent.createEvent(
            EventType.ORDER_CREATED.name(),
            "{\"skuKey\":1,\"finalStockCount\":90,\"timestamp\":\"2025-04-02T10:00:00\"}"
        );
        when(eventCreator.createStockEvents(any(), anyLong(), anyInt(), anyLong()))
            .thenReturn(List.of(mockEvent));

        // When
        orderService.createOrder(dto, idempotencyKey);

        // Then
        verify(eventCreator).createStockEvents(eq(EventType.ORDER_CREATED), eq(1L), eq(90), eq(1L));
        verify(eventPublisher).publishEvent(any(OutboxEvent.class));
    }
}