package org.project.ecommerce.fulfillment.domain;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.LocalDateTime;
@Getter
@Data
@Table("stocks")
public class Stock {
    @PrimaryKey("sku_key")
    private Long skuKey;

    @Column("stock_count")
    private int stockCount;

    @Column("updated_at")
    private LocalDateTime updatedAt;



}
