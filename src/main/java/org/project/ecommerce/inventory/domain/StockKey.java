package org.project.ecommerce.inventory.domain;

import lombok.*;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;

import java.io.Serializable;

@PrimaryKeyClass
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class StockKey implements Serializable {

    @PrimaryKeyColumn(name = "vendor_item_key", type = PrimaryKeyType.PARTITIONED)
    private Long vendorItemKey;

    @PrimaryKeyColumn(name = "sku_key", type = PrimaryKeyType.CLUSTERED)
    private Long skuKey;

//    private StockKey(Long vendorItemKey, Long skuKey) {
//        this.vendorItemKey = vendorItemKey;
//        this.skuKey = skuKey;
//    }


}
