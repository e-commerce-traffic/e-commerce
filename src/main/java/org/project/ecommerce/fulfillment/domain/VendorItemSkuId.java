package org.project.ecommerce.fulfillment.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class VendorItemSkuId implements Serializable {
    private Long vendorItem; // VendorItem의 ID
    private Long sku;        // Sku의 ID
}