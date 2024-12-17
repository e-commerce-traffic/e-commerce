    package org.project.ecommerce.order.application;

import lombok.RequiredArgsConstructor;
import org.project.ecommerce.fulfillment.domain.*;
import org.project.ecommerce.order.domain.*;
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
    private final RedisTemplate<String,String> redisTemplate;
    private final VendorItemRepository vendorItemRepository;
    private final SkuRepository skuRepository;
    private final InboundRepository inboundRepository;
    private final OrderRepository orderRepository;

    private static final String REDIS_STOCK_KEY_PREFIX = "stock:vendorItem:";
    /**
     * 주문 생성 로직
     * @param dto userId, vendor_item_id, count 리스트 포함
     * @param idempotencyKey 멱등성 키
     */
    @Transactional
    public void createOrder(OrderRequestDto dto, String idempotencyKey) {
        // 1. 멱등성 체크
        if (orderRepository.existsByIdempotencyKey(idempotencyKey)) {
            // 이미 처리된 주문
            throw new IllegalStateException("Order with this idempotencyKey already exists.");
        }

        // 2. Order 엔티티 생성 (아직 OrderItem은 추가 안함)
        Order order = Order.create(dto.getUserId(), idempotencyKey, new ArrayList<>());

        // 3. 요청된 각 vendorItem에 대해 재고 확보 로직
        for (OrderItemDto itemDto : dto.getOrderItem()) {
            Long vendorItemId = itemDto.getVendorItemId();
            int requiredCount = itemDto.getItemCount();

            // 3-1. 해당 vendorItem 정보 조회
            VendorItem vendorItem = vendorItemRepository.findById(vendorItemId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid VendorItem ID: " + vendorItemId));

            // 3-2. vendorItem에 연결된 모든 SKU 조회
            List<VendorItemSku> vendorItemSkus = vendorItemSkuRepository.findByVendorItem_Id(vendorItemId);
            if (vendorItemSkus.isEmpty()) {
                throw new IllegalArgumentException("No SKU associated with VendorItem: " + vendorItemId);
            }

            // 현재 예제에서는 단순히 하나의 SKU로 재고 차감한다고 가정하거나,
            // 여러 SKU가 있을 경우 InboundDate 기준으로 모두 섞어서 차감하려면 SKU 목록 추출 필요
            List<Sku> skus = vendorItemSkus.stream().map(VendorItemSku::getSku).collect(Collectors.toList());

            // 3-3. 해당 SKU들에 대한 Inbound 리스트를 오래된 순서대로 조회
            List<Inbound> inbounds = inboundRepository.findAllBySkuInOrderByInboundDateAsc(skus);

            // FIFO 방식으로 requiredCount 소진
            int remain = requiredCount;
            for (Inbound inbound : inbounds) {
                int available = inbound.getItemCount();
                if (available == 0) continue;

                if (available >= remain) {
                    // 이 inbound에서 모두 충당 가능
                    inbound.setItemCount(available - remain);  // inbound itemCount 차감
                    inbound.getSku().setStockCount(inbound.getSku().getStockCount() - remain); // SKU 재고 차감
                    skuRepository.save(inbound.getSku());
                    inboundRepository.save(inbound);
                    remain = 0;
                    break;
                } else {
                    // 현재 inbound로 다 충당 불가, 남은 부분은 다음 inbound로 넘어감
                    inbound.setItemCount(0);
                    inbound.getSku().setStockCount(inbound.getSku().getStockCount() - available);
                    skuRepository.save(inbound.getSku());
                    inboundRepository.save(inbound);
                    remain -= available;
                }
            }

            if (remain > 0) {
                // 재고 부족
                throw new IllegalStateException("Not enough stock for VendorItem ID: " + vendorItemId);
            }

            // 재고 차감 성공 시 OrderItem 생성
            // SKU를 1개만 차감하거나, 여러 SKU 중 어떤 SKU 차감했는지 기록 필요하다면
            // 여기서는 단순히 첫 번째 VendorItemSku의 SKU를 사용(정책에 따라 수정 가능)
            Sku chosenSku = vendorItemSkus.get(0).getSku();

            OrderItem orderItem = OrderItem.create(vendorItem, chosenSku, requiredCount, order);
            order.getOrderItems().add(orderItem);
        }

        // 4. Order 저장
        orderRepository.save(order);

        // 5. Redis 캐시 업데이트: 차감한 수량 만큼 vendorItemId별 재고 감소
        // vendorItemId별로 합산 재고를 이미 캐시해두었다고 가정
        for (OrderItemDto itemDto : dto.getOrderItem()) {
            String cacheKey = REDIS_STOCK_KEY_PREFIX + itemDto.getVendorItemId();
            Integer cachedStock = (Integer) redisTemplate.opsForValue().get(cacheKey);
            if (cachedStock != null) {
                // 재고 차감
                int newStock = cachedStock - itemDto.getItemCount();
                if (newStock < 0) newStock = 0;
                redisTemplate.opsForValue().set(cacheKey, newStock);
            } else {
                // 캐시가 없으면 무시하거나, DB에서 재조회 후 캐싱할 수도 있음.
                // 여기서는 단순히 무시(또는 나중에 sync batch로 갱신)
            }
        }

        // 여기까지 완료되면 주문 생성 및 재고 차감, 캐시 업데이트 완료
    }

}
