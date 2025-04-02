package org.project.ecommerce.order.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ecommerce.common.infrastructure.outbox.EventType;
import org.project.ecommerce.common.infrastructure.outbox.OutBoxEventRepository;
import org.project.ecommerce.common.infrastructure.outbox.OutboxEventCreator;
import org.project.ecommerce.common.infrastructure.route.ShardRouter;
import org.project.ecommerce.fulfillment.application.StockService;
import org.project.ecommerce.fulfillment.domain.*;
import org.project.ecommerce.order.domain.*;
import org.project.ecommerce.order.ui.dto.OrderItemDto;
import org.project.ecommerce.order.ui.dto.OrderRequestDto;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


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
        // 멱등성 체크 - 동일 idempotencyKey로 중복 요청 방지
        validateIdempotency(idempotencyKey);

        // 주문 엔티티 생성 및 저장 (ID 생성 목적)
        Order order = Order.create(dto.getUserId(), idempotencyKey);
        order = orderRepository.save(order);

        // 샤딩 - 주문 ID 기반으로 적절한 샤드 설정
        ShardRouter.setShardKey(order.getId());
        log.debug("Order {} routed to shard based on ID hash", order.getId());

        // 주문 아이템 처리
        for (OrderItemDto itemDto : dto.getOrderItem()) {
            // 상품 조회
            VendorItem vendorItem = vendorItemRepository.findById(itemDto.getVendorItemId())
                    .orElseThrow(() -> new IllegalStateException("상품을 찾을 수 없습니다."));

            // 총 재고 확인 (Cassandra 기반)
            if (stockService.getStockByVendorItemId(vendorItem.getId()) < itemDto.getItemCount()) {
                throw new IllegalStateException("재고가 부족합니다.");
            }

            // 충분한 재고가 있는 SKU 선택
            Optional<VendorItemSku> availableSku = vendorItem.getVendorItemSkus().stream()
                    .filter(vis -> vis.getSku().getStockCount() >= itemDto.getItemCount())
                    .findFirst();

            if (availableSku.isEmpty()) {
                throw new IllegalStateException("단일 SKU의 재고가 부족합니다.");
            }

            Sku selectedSku = availableSku.get().getSku();
            int originalStock = selectedSku.getStockCount();

            // 재고 차감 및 저장
            selectedSku.decreaseStock(itemDto.getItemCount());
            skuRepository.save(selectedSku);

            log.info("SKU id {}: Stock decreased by {}. Before: {}, After: {}",
                    selectedSku.getId(), itemDto.getItemCount(), originalStock, selectedSku.getStockCount());

            // 주문 아이템 추가
            order.addOrderItem(vendorItem, selectedSku, itemDto.getItemCount());

            eventCreator.createStockEvents(
                    EventType.ORDER_CREATED,
                    selectedSku.getId(),
                    selectedSku.getStockCount(),
                    vendorItem.getId()
            ).forEach(eventPublisher::publishEvent);
        }

        // 주문 최종 저장
        Order savedOrder = orderRepository.save(order);

        // 아웃박스 패턴: 동일 트랜잭션 내에서 이벤트 생성
        // 이렇게 함으로써 DB 업데이트와 이벤트 생성이 원자적으로 처리됨
//        createAndPublishEvents(savedOrder);
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
