package org.project.ecommerce.fulfillment.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.project.ecommerce.order.domain.VendorItem;

import java.util.ArrayList;
import java.util.List;


@Getter
@Table(name = "sku")
@Entity
@NoArgsConstructor
public class Sku {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sku_id")
    private Long id;


    @OneToMany(mappedBy = "sku", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VendorItemSku> vendorItemSkus = new ArrayList<>(); // N:M 관계를 명시적으로 설정

    @Column(name = "stock_count", nullable = false) // 재고 관리 필드 추가
    private int stockCount=0;

    public void incrementStock(int count) {
        if (count < 0) throw new IllegalArgumentException("Increment count cannot be negative");
        this.stockCount += count;
    }

    public void decrementStock(int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("Decrement count must be positive");
        }
        if (this.stockCount < count) {
            throw new IllegalStateException("Not enough stock");
        }
        this.stockCount -= count;
    }

    @Builder
    public Sku(Long id, List<VendorItemSku> vendorItemSkus, int stockCount) {
        this.id = id;
        this.vendorItemSkus = vendorItemSkus;
        this.stockCount = stockCount;
    }

}
