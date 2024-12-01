package org.project.ecommerce.order.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@Table(name = "vendor_item")
@Entity
@NoArgsConstructor
public class VendorItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vendor_item_id")
    private Long id;

    @Column
    private String name;

    @OneToMany(mappedBy = "vendorItem")
    private List<Sku> sku;

    @Column
    private int quantity;

    @Builder
    public VendorItem(Long id, String name, int quantity, List<Sku> sku) {
        this.id = id;
        this.name = name;
        this.quantity = quantity;
        this.sku = sku;

    }


}
