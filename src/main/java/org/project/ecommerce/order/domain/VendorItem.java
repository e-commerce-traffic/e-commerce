package org.project.ecommerce.order.domain;

import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class VendorItem {
    private final Long id;
    private final String name;
    private final List<Sku> skus = new ArrayList<>();

    @Builder
    public VendorItem(Long id,String name) {
        this.id = id;
        this.name=name;
    }
}
