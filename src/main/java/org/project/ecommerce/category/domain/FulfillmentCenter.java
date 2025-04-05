package org.project.ecommerce.category.domain;

import jakarta.persistence.*;
import lombok.Getter;
import org.project.ecommerce.inventory.domain.Inbound;

import java.util.ArrayList;
import java.util.List;


@Getter
@Table(name = "fulfillment_center")
@Entity
public class FulfillmentCenter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fulfillment_center_id")
    private Long id;

    @Column
    private String name;

}
