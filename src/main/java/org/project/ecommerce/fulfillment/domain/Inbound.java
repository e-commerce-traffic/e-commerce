package org.project.ecommerce.fulfillment.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


import java.time.LocalDateTime;

@Getter
@Table(name = "inbound")
@Entity
@NoArgsConstructor
public class Inbound {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inbound_id")
    private Long id;

    @Column(name = "item_count", nullable = false)
    private int itemCount;

    @Column(name = "inbound_date", nullable = false)
    private LocalDateTime inboundDate;

    @ManyToOne(optional = false)
    @JoinColumn(name = "fulfillment_center_id", nullable = false)
    private FulfillmentCenter fulfillmentCenter;

    @ManyToOne(optional = false)
    @JoinColumn(name = "sku_id", nullable = false)
    private Sku sku;



    public static Inbound create(Sku sku, FulfillmentCenter fulfillmentCenter, int itemCount) {
        return Inbound.builder()
                .sku(sku)
                .fulfillmentCenter(fulfillmentCenter)
                .itemCount(itemCount)
                .inboundDate(LocalDateTime.now())
                .build();
    }

    public void updateItemCount(int newCount) {
        if (newCount < 0) {
            throw new IllegalArgumentException("Item count cannot be negative");
        }
        this.itemCount = newCount;
    }

    @Builder
    public Inbound(Sku sku, FulfillmentCenter fulfillmentCenter, int itemCount, LocalDateTime inboundDate) {
        this.sku = sku;
        this.fulfillmentCenter = fulfillmentCenter;
        this.itemCount = itemCount;
        this.inboundDate = inboundDate;
    }

}
