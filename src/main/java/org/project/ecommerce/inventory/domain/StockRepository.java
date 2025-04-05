package org.project.ecommerce.inventory.domain;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface StockRepository extends CassandraRepository<Stock, StockKey> {


    @Query("SELECT stock_count FROM stocks WHERE sku_key = :skuKey")
    Integer findStockCountBySkuKey(Long skuKey);


    @Query("SELECT SUM(stock_count) FROM stocks WHERE vendor_item_key = :vendorItemKey")
    int findTotalStockByVendorItemId( Long vendorItemKey);

    @Query("SELECT * FROM stocks WHERE sku_key = :skuKey AND vendor_item_key = :vendorItemKey")
    Stock findBySkuKeyAndVendorItemKey(
            Long skuKey,
            Long vendorItemKey
    );

       @Query("UPDATE stocks SET stock_count = :newCount, updated_at = :timestamp " +
            "WHERE vendor_item_key = :vendorItemKey AND sku_key = :skuKey")
    void updateStockCountAndTimestamp(Long vendorItemKey,Long skuKey, int newCount, LocalDateTime timestamp);





}
