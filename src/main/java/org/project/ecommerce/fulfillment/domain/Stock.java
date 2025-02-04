package org.project.ecommerce.fulfillment.domain;

import lombok.Builder;
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

    @PrimaryKeyColumn(name = "vendor_item_key", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
    private Long vendorItemKey;

    @PrimaryKeyColumn(name = "sku_key", ordinal = 1, type = PrimaryKeyType.CLUSTERED)
    private Long skuKey;

    @Column("stock_count")
    private int stockCount;

    @Column("updated_at")
    private LocalDateTime updatedAt;



}
