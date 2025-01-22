package org.project.ecommerce.fulfillment.domain;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;


import java.time.LocalDateTime;
import java.util.List;

public interface StockRepository extends CassandraRepository<Stock,Long> {
    @Query("SELECT stock_count FROM stocks WHERE sku_key = :skuKey")
    Integer findStockBySkuKey(Long skuKey);

    @Query("SELECT stock_count FROM stocks WHERE sku_key = :skuKey")
    Integer findStockCountBySkuKey(Long skuKey);

//    @Query("UPDATE stocks SET stock_count = :newCount WHERE sku_key = :skuKey")
//    void updateStockCount(Long skuKey, int newCount);


    @Query("UPDATE stocks SET stock_count = :newCount WHERE sku_key = :skuKey IF stock_count = :expectedCount")
    boolean updateStockCount(Long skuKey, int newCount, int expectedCount);


//    @Query("UPDATE stocks SET stock_count = :newCount, updated_at = :updatedAt " +
//            "WHERE sku_key = :skuKey IF stock_count = :expectedCount")
//    boolean updateStockCount(Long skuKey, int newCount, int expectedCount, LocalDateTime updatedAt);
}
