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

    @Transactional
    public void createOrder(OrderRequestDto dto, String idempotencyKey) {
        if (redisTemplate.hasKey(idempotencyKey)) {
            throw new IllegalArgumentException("Duplicate request: Idempotency key already exists");
        }
        redisTemplate.opsForValue().set(idempotencyKey, "LOCKED", Duration.ofSeconds(300));

        // 2. 주문 데이터 생성
        List<OrderItem> orderItems = dto.getOrderItem().stream()
                .flatMap(itemDto -> {
                    // VendorItem 조회
                    VendorItem vendorItem = vendorItemRepository.findById(itemDto.getVendorItemId())
                            .orElseThrow(() -> new IllegalArgumentException("Invalid vendor item ID: " + itemDto.getVendorItemId()));

                    // VendorItem에 해당하는 SKU 목록 조회
                    List<Sku> skus = skuRepository.findByVendorItemId(vendorItem.getId());
                    if (skus.isEmpty()) {
                        throw new IllegalArgumentException("No SKUs found for vendor item ID: " + vendorItem.getId());
                    }

                    int remainingCount = itemDto.getItemCount();
                    List<OrderItem> items = new ArrayList<>();
                    for (Sku sku : skus) {
                        List<Inbound> inbounds = inboundRepository.findBySkuId(sku.getId());

                        for (Inbound inbound : inbounds) {
                            if (remainingCount == 0) break;

                            if (inbound.getItemCount() >= remainingCount) {
                                inbound.updateItemCount(inbound.getItemCount() - remainingCount);
                                inboundRepository.save(inbound); // 기존 객체를 업데이트

                                items.add(OrderItem.builder()
                                        .vendorItem(vendorItem)
                                        .sku(sku)
                                        .itemCount(remainingCount)
                                        .build()); // Order 객체는 나중에 설정
                                remainingCount = 0;
                                break;
                            } else {
                                int usedStock = inbound.getItemCount();

                                // Inbound의 재고 모두 사용
                                inbound.updateItemCount(0);
                                inboundRepository.save(inbound);

                                items.add(OrderItem.builder()
                                        .vendorItem(vendorItem)
                                        .sku(sku)
                                        .itemCount(usedStock)
                                        .build());
                                remainingCount -= usedStock;
                            }
                        }
                    }

                    if (remainingCount > 0) {
                        throw new IllegalStateException("재고가 부족합니다. vendor item ID: " + itemDto.getVendorItemId());
                    }

                    return items.stream(); // 생성된 OrderItem Stream 반환
                })
                .collect(Collectors.toList());

        // 3. Order 저장
        Order order = Order.create(dto.getUserId(), idempotencyKey, new ArrayList<>()); // 우선 Order 객체를 생성하고 나중에 OrderItem 추가
        for (OrderItem item : orderItems) {
            item.setOrder(order);
            order.getOrderItems().add(item);
        }

        orderRepository.save(order);
    }

}
