package org.project.ecommerce.fulfillment.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import org.project.ecommerce.order.domain.VendorItem;


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
