package org.project.ecommerce.order.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;


@Getter
@Table(name = "sku")
@Entity
public class Sku {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sku_id")
    private Long id;


    @ManyToOne
    @JoinColumn(name = "vendor_item_id")
    private VendorItem vendorItem;


}
