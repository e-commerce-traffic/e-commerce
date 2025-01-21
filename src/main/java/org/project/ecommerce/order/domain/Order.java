package org.project.ecommerce.order.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.project.ecommerce.common.infrastructure.outbox.OutboxEvent;
import org.project.ecommerce.common.infrastructure.utils.JsonUtils;
import org.project.ecommerce.fulfillment.domain.Sku;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Entity
@Table(name = "orders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // Builder나 생성자는 Order 객체 초기 생성 시 한 번만 사용
    @Builder
    private Order(Long userId, String idempotencyKey, LocalDateTime createdAt) {
        this.userId = userId;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = createdAt;
    }

    public static Order create(Long userId, String idempotencyKey) {
        return Order.builder()
                .userId(userId)
                .idempotencyKey(idempotencyKey)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // 연관관계 편의 메서드: 여기서 OrderItem을 생성하고 바로 리스트에 추가
    public void addOrderItem(VendorItem vendorItem, Sku sku, int itemCount) {
        OrderItem orderItem = OrderItem.create(vendorItem, sku, itemCount, this);
        this.orderItems.add(orderItem);
    }

    private OutboxEvent createStockUpdateEvent(OrderItem orderItem) {
        Map<String, Object> payload = new HashMap<>();

        payload.put("skuKey", orderItem.getSku().getId());
        payload.put("decreasedCount", orderItem.getItemCount());

        return OutboxEvent.createEvent("OrderCreated", JsonUtils.toJson(payload));
    }
}
