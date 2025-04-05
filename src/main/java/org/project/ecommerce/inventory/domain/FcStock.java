package org.project.ecommerce.inventory.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Table(name = "fc_stock")
@Entity
@NoArgsConstructor
public class FcStock {
    @EmbeddedId
    private FcStockId id;

    @Column(name = "stock_count", nullable = false)
    private int stockCount;


    public static FcStock create(FcStockId id, int initialStock) {
        if (initialStock < 0) throw new IllegalArgumentException("Initial stock cannot be negative");
        FcStock stock = new FcStock();
        stock.id = id;
        stock.stockCount = initialStock;
        return stock;
    }


    public void incrementStock(int count) {
        if (count < 0) throw new IllegalArgumentException("Increment count cannot be negative");
        this.stockCount += count;
    }


    public void decreaseStock(int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("Decrement count must be positive");
        }
        if (this.stockCount < count) {
            throw new IllegalStateException("Not enough stock");
        }
        this.stockCount -= count;
    }

}
