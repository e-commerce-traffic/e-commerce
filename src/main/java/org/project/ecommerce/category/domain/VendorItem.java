package org.project.ecommerce.category.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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


    @Column(name = "created_at")
    private LocalDateTime createdAt;


}
