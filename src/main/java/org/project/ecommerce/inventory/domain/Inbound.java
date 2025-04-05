package org.project.ecommerce.inventory.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.project.ecommerce.category.domain.FulfillmentCenter;
import org.project.ecommerce.category.domain.Sku;


import java.time.LocalDateTime;

@Getter
@Table(name = "inbound")
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inbound {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inbound_id")
    private Long id;

    @Column(name = "item_count", nullable = false)
    private int itemCount;

    @Column(name = "inbound_date", nullable = false)
    private LocalDateTime inboundDate;

    @Column(name = "fulfillment_center_id", nullable = false)
    private Long fulfillmentCenterId;

    @Column(name = "sku_id", nullable = false)
    private Long skuId;

    Inbound(Long skuId, Long fulfillmentCenterId, int itemCount, LocalDateTime inboundDate) {
        this.skuId = skuId;
        this.fulfillmentCenterId = fulfillmentCenterId;
        this.itemCount = itemCount;
        this.inboundDate = inboundDate;
    }

    public static Inbound create(Long fulfillmentCenterId, Long skuId, int itemCount) {
        return new Inbound(skuId, fulfillmentCenterId, itemCount, LocalDateTime.now());
    }


}
