package org.project.ecommerce.order.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.project.ecommerce.fulfillment.domain.Sku;
import org.project.ecommerce.fulfillment.domain.VendorItemSku;

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


    @OneToMany(mappedBy = "vendorItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VendorItemSku> vendorItemSkus = new ArrayList<>(); // N:M 관계를 명시적으로 설정

    @Builder
    public VendorItem(Long id, String name, List<VendorItemSku> vendorItemSkus) {
        this.id = id;
        this.name = name;
        this.vendorItemSkus = vendorItemSkus;

    }


}
