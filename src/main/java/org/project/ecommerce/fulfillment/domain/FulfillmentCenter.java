package org.project.ecommerce.fulfillment.domain;

import jakarta.persistence.*;
import lombok.Getter;

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

    @OneToMany(mappedBy = "fulfillmentCenter")
    private List<Inbound> inbound;


}
