package org.project.ecommerce.order.ui.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.project.ecommerce.order.domain.Order;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
public class OrderCreatedEvent {
    private Long orderId;
    private List<OrderItemEvent> orderItems;

    @Builder
    public OrderCreatedEvent(Long orderId, List<OrderItemEvent> orderItems) {
        this.orderId = orderId;
        this.orderItems = orderItems;
    }

    public static OrderCreatedEvent from(Order order) {
        return OrderCreatedEvent.builder()
                .orderId(order.getId())
                .orderItems(order.getOrderItems().stream()
                        .map(OrderItemEvent::from)
                        .collect(Collectors.toList()))
                .build();
    }
}
