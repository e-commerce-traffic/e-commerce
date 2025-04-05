package org.project.ecommerce.inventory.domain;

import lombok.Data;
import lombok.Getter;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.LocalDateTime;

@Getter
@Data
@Table("stocks")
public class Stock {

    @PrimaryKey
    private StockKey stockKey;

    @Column("stock_count")
    private int stockCount;

    @Column("updated_at")
    private LocalDateTime updatedAt;


    public Stock(StockKey stockKey, int stockCount, LocalDateTime updatedAt) {
        this.stockKey = stockKey;
        this.stockCount = stockCount;
        this.updatedAt = updatedAt;
    }

    public static Stock create(StockKey stockKey, int stockCount, LocalDateTime updatedAt) {
        if (stockKey == null || updatedAt == null) {
            throw new IllegalArgumentException("Stock 생성에 필요한 필드가 비어있습니다.");
        }
        return new Stock(stockKey, stockCount, updatedAt);
    }

    public void applyDelta(int delta, LocalDateTime eventTime) {
        this.stockCount += delta;
        this.updatedAt = eventTime;
    }

}
