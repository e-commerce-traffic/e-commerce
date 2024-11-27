package org.project.ecommerce.fulfillment.domain;

import lombok.Builder;
import lombok.Getter;
import org.project.ecommerce.order.domain.Sku;

import java.time.LocalDateTime;

@Getter
public class Inbound {
    private final Long id;
    private final Sku sku;
    private final FulfillmentCenter fulfillmentCenter;
    private final int quantity;
    private final LocalDateTime inboundDate;

    @Builder
    public Inbound(Long id, Sku sku, FulfillmentCenter fulfillmentCenter, int quantity, LocalDateTime inboundDate) {
        this.id = id;
        this.sku = sku;
        this.fulfillmentCenter = fulfillmentCenter;
        this.quantity = quantity;
        this.inboundDate = inboundDate;
    }
}
