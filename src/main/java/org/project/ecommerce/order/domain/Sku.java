package org.project.ecommerce.order.domain;

import lombok.Builder;
import lombok.Getter;


@Getter
public class Sku {
    private final Long id;
    private final String name;
    private final VendorItem vendorItem;


    @Builder
    public Sku(Long id, String name, VendorItem vendorItem) {
        this.id = id;
        this.name = name;
        this.vendorItem = vendorItem;

    }
}
