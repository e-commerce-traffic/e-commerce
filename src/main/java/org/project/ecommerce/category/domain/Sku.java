package org.project.ecommerce.category.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Getter
@Table(name = "sku")
@Entity
@NoArgsConstructor
public class Sku {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sku_id")
    private Long id;

    @Column(name = "sku_name")
    private String skuName;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

}
