package org.project.ecommerce.category.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;


import java.io.Serializable;

@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class VendorItemSkuId implements Serializable {
    @Column(name = "vendor_item_id")
    private Long vendorItemId;

    @Column(name = "sku_id")
    private Long skuId;
}
