package org.project.ecommerce.fulfillment.domain;

import lombok.Builder;
import lombok.Getter;
import org.project.ecommerce.order.domain.Sku;
@Getter
public class Stock {
    private final Long id;
    private final Sku sku;
    private final FulfillmentCenter fulfillmentCenter;
    private int quantity;
@Builder
    public Stock(Long id, Sku sku, FulfillmentCenter fulfillmentCenter,int quantity) {
        this.id = id;
        this.sku = sku;
        this.fulfillmentCenter = fulfillmentCenter;
        this.quantity = quantity;
    }
}
