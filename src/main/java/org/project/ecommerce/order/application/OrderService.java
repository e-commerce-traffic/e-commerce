package org.project.ecommerce.order.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ecommerce.common.infrastructure.outbox.EventType;
import org.project.ecommerce.common.infrastructure.outbox.OutBoxEventRepository;
import org.project.ecommerce.common.infrastructure.outbox.OutboxEventCreator;
import org.project.ecommerce.common.infrastructure.utils.JsonUtils;
import org.project.ecommerce.fulfillment.application.StockService;
import org.project.ecommerce.fulfillment.domain.*;
import org.project.ecommerce.order.domain.*;
import org.project.ecommerce.common.infrastructure.outbox.OutboxEvent;
import org.project.ecommerce.order.ui.dto.OrderItemDto;
import org.project.ecommerce.order.ui.dto.OrderRequestDto;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    private final VendorItemRepository vendorItemRepository;
    private final SkuRepository skuRepository;
    private final InboundRepository inboundRepository;
    private final OrderRepository orderRepository;
    private final VendorItemSkuRepository vendorItemSkuRepository;
    private final OutBoxEventRepository outboxRepository;
    private final OutboxEventCreator eventCreator;


    private final StockService stockService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 주문 생성 로직
     *
     * @param dto            {userId, vendor_item_id, count}
     * @param idempotencyKey 멱등성 키
     */
    @Transactional
    public void createOrder(OrderRequestDto dto, String idempotencyKey) {
        validateIdempotency(idempotencyKey);
        Order order = Order.create(dto.getUserId(), idempotencyKey);
        orderRepository.save(order);
        for (OrderItemDto itemDto : dto.getOrderItem()) {
            VendorItem vendorItem = vendorItemRepository.findById(itemDto.getVendorItemId())
                    .orElseThrow(() -> new IllegalStateException("상품을 찾을 수 없습니다."));

            List<Long> skuIds = vendorItem.getVendorItemSkus().stream()
                    .map(v -> v.getSku().getId()).collect(Collectors.toList());

            // Cassandra에서 재고 조회
            if (stockService.getStockByVendorItemId(vendorItem.getId()) < itemDto.getItemCount()) {
                throw new IllegalStateException("재고가 부족합니다.");
            }
            // 재고가 충분한 SKU 선택
            Optional<VendorItemSku> availableSku = vendorItem.getVendorItemSkus().stream()
                    .filter(vis -> vis.getSku().getStockCount() >= itemDto.getItemCount())
                    .findFirst();

            if (availableSku.isEmpty()) {
                throw new IllegalStateException("단일 SKU의 재고가 부족합니다.");
            }

            Sku selectedSku = availableSku.get().getSku();
            selectedSku.decreaseStock(itemDto.getItemCount());
            skuRepository.save(selectedSku);


            order.addOrderItem(vendorItem, selectedSku, itemDto.getItemCount());
        }


        Order savedOrder = orderRepository.save(order);
        createAndPublishEvents(savedOrder);
    }

    private void createAndPublishEvents(Order savedOrder) {
        savedOrder.getOrderItems().forEach(orderItem -> {
            eventCreator.createStockEvents(
                    EventType.ORDER_CREATED,
                    orderItem.getSku().getId(),
                    orderItem.getSku().getStockCount(),
                    orderItem.getVendorItem().getId()
            ).forEach(eventPublisher::publishEvent);
        });
    }


    private void validateIdempotency(String idempotencyKey) {
        if (orderRepository.existsByIdempotencyKey(idempotencyKey)) {
            throw new IllegalStateException("Order with this idempotencyKey already exists.");
        }
    }

}
