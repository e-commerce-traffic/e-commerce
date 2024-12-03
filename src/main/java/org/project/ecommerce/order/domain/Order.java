package org.project.ecommerce.order.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "orders")
@NoArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(name = "idempotency_id", nullable = false)
    private String idempotencyKey;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public Order(Long userId, String idempotencyKey, LocalDateTime createdAt) {
        this.userId = userId;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = createdAt;
    }


    public static Order create(Long userId, String idempotencyKey, List<OrderItem> orderItems) {
        Order order = Order.builder()
                .userId(userId)
                .idempotencyKey(idempotencyKey)
                .createdAt(LocalDateTime.now())
                .build();

        orderItems.forEach(item -> order.addOrderItem(
                OrderItem.create(item.getVendorItem(), item.getSku(), item.getItemCount(), order)
        ));

        return order;
    }

    // 연관 관계 편의 메서드
    private void addOrderItem(OrderItem orderItem) {
        orderItems.add(orderItem);
    }
}
