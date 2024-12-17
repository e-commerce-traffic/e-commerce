package org.project.ecommerce.order.application;

import lombok.RequiredArgsConstructor;
import org.project.ecommerce.fulfillment.domain.*;
import org.project.ecommerce.order.domain.*;
import org.project.ecommerce.order.ui.dto.OrderItemDto;
import org.project.ecommerce.order.ui.dto.OrderRequestDto;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final VendorItemRepository vendorItemRepository;
    private final SkuRepository skuRepository;
    private final InboundRepository inboundRepository;
    private final OrderRepository orderRepository;
    private final VendorItemSkuRepository vendorItemSkuRepository;

    private static final String REDIS_STOCK_KEY_PREFIX = "stock:vendorItem:";

    /**
     * 주문 생성 로직
     *
     * @param dto            userId, vendor_item_id, count 리스트 포함
     * @param idempotencyKey 멱등성 키
     */
    @Transactional
    public void createOrder(OrderRequestDto dto, String idempotencyKey) {
        // 1. 멱등성 체크
        // idempotencyKey 컬럼에 Unique 제약이 있어야 함
        if (orderRepository.existsByIdempotencyKey(idempotencyKey)) {
            throw new IllegalStateException("Order with this idempotencyKey already exists.");
        }

        // 2. Order 엔티티 생성
        Order order = Order.create(dto.getUserId(), idempotencyKey);

        for (OrderItemDto itemDto : dto.getOrderItem()) {
            Long vendorItemId = itemDto.getVendorItemId();
            int requiredCount = itemDto.getItemCount();

            // VendorItem 조회
            VendorItem vendorItem = vendorItemRepository.findById(vendorItemId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid VendorItem ID: " + vendorItemId));

            // VendorItem에 연결된 SKU 조회
            List<VendorItemSku> vendorItemSkus = vendorItemSkuRepository.findByVendorItem_Id(vendorItemId);
            if (vendorItemSkus.isEmpty()) {
                throw new IllegalArgumentException("No SKU associated with VendorItem: " + vendorItemId);
            }

            // 여러 SKU가 매핑되어 있을 경우, FIFO 처리를 위해 모든 SKU의 Inbound를 수집
            // 여기서는 단순히 모든 SKU에 대한 Inbound를 오래된 순으로 가져와 총 재고를 확인한다고 가정
            List<Sku> skus = vendorItemSkus.stream().map(VendorItemSku::getSku).collect(Collectors.toList());
            List<Inbound> inbounds = inboundRepository.findAllBySkuInOrderByInboundDateAsc(skus);

            // FIFO 논리 검증: inbound의 itemCount 합으로 총 유입량 파악
            // 실제로 inbound.itemCount는 수정하지 않고, 단지 충분한 재고가 있었는지 확인하는 용도
            int totalAvailableFromInboundHistory = 0;
            for (Inbound inbound : inbounds) {
                totalAvailableFromInboundHistory += inbound.getItemCount();
                if (totalAvailableFromInboundHistory >= requiredCount) {
                    // 오래된 재고부터 충분히 충당 가능하다고 가정 (논리적 FIFO 충족)
                    break;
                }
            }

            // SKU 실재고 확인
            // 모든 SKU stockCount 합산
            int totalSkuStock = skus.stream().mapToInt(Sku::getStockCount).sum();

            if (totalAvailableFromInboundHistory < requiredCount || totalSkuStock < requiredCount) {
                // 재고 부족
                throw new IllegalStateException("Not enough stock for VendorItem ID: " + vendorItemId);
            }

            // FIFO 논리상 재고는 충분하므로, 여기서는 실제 stockCount에서 차감만 하면 된다.
            // 어떤 SKU에서 얼마나 차감할지는 정책에 따라 달라질 수 있으나,
            // 여기서는 단순히 첫 번째 SKU에서 차감한다고 가정 (정책 필요)
            Sku chosenSku = vendorItemSkus.get(0).getSku();
            chosenSku.decrementStock(chosenSku.getStockCount() - requiredCount);
            skuRepository.save(chosenSku);

//            // OrderItem 생성
//            OrderItem orderItem = OrderItem.create(vendorItem, chosenSku, requiredCount, order);
//            order.getOrderItems().add(orderItem);

            // OrderItem 추가
            order.addOrderItem(vendorItem, chosenSku, requiredCount);


        }

        orderRepository.save(order);

        // Redis 캐시 업데이트
        for (OrderItemDto itemDto : dto.getOrderItem()) {
            String cacheKey = REDIS_STOCK_KEY_PREFIX + itemDto.getVendorItemId();
            Integer cachedStock = (Integer) redisTemplate.opsForValue().get(cacheKey);
            if (cachedStock != null) {
                int newStock = cachedStock - itemDto.getItemCount();
                if (newStock < 0) newStock = 0;
                redisTemplate.opsForValue().set(cacheKey, newStock);
            } else {
                // 캐시에 없을 경우, 생략하거나 나중에 별도로 동기화
            }
        }
    }

}
