package org.project.ecommerce.order.ui.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.project.ecommerce.order.domain.OrderItem;

@Getter
@NoArgsConstructor
public class OrderItemEvent {
    private Long skuId;
    private int count;

    @Builder
    public OrderItemEvent(Long skuId, int count) {
        this.skuId = skuId;
        this.count = count;
    }

    public static OrderItemEvent from(OrderItem orderItem) {
        return OrderItemEvent.builder()
                .skuId(orderItem.getSku().getId())
                .count(orderItem.getItemCount())
                .build();
    }
}
