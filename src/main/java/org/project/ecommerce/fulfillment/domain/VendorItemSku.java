package org.project.ecommerce.fulfillment.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.project.ecommerce.order.domain.VendorItem;

@Getter
@Table(name = "vendor_item_sku")
@Entity
@IdClass(VendorItemSkuId.class)
@NoArgsConstructor
public class VendorItemSku {
    @Id
    @ManyToOne
    @JoinColumn(name = "vendor_item_id", nullable = false)
    private VendorItem vendorItem;

    @Id
    @ManyToOne
    @JoinColumn(name = "sku_id", nullable = false)
    private Sku sku;


    @Builder
    public VendorItemSku(VendorItem vendorItem, Sku sku) {
        this.vendorItem = vendorItem;
        this.sku = sku;
    }
}
