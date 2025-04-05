package org.project.ecommerce.category.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Table(name = "vendor_item_sku")
@Entity
@NoArgsConstructor
public class VendorItemSku {

    @EmbeddedId
    private VendorItemSkuId id;


    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("vendorItemId")
    @JoinColumn(name = "vendor_item_id")
    private VendorItem vendorItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("skuId")
    @JoinColumn(name = "sku_id")
    private Sku sku;

    @Column(nullable = false)
    private int quantity;
}
