package org.project.ecommerce.fulfillment.domain;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;


import java.time.LocalDateTime;
import java.util.List;

public interface StockRepository extends CassandraRepository<Stock, Long> {


    @Query("SELECT stock_count FROM stocks WHERE sku_key = :skuKey")
    Integer findStockCountBySkuKey(Long skuKey);

    @Query("SELECT * FROM stocks WHERE sku_key = :skuKey")
    Stock findBySkuKey(Long skuKey);

    @Query("SELECT SUM(stock_count) FROM stocks WHERE vendor_item_key = :vendorItemKey")
    int findTotalStockByVendorItemId( Long vendorItemKey);

    @Query("SELECT * FROM stocks WHERE sku_key = :skuKey AND vendor_item_key = :vendorItemKey")
    Stock findBySkuKeyAndVendorItemKey(
            Long skuKey,
            Long vendorItemKey
    );

    @Query("UPDATE stocks SET stock_count = :newCount WHERE sku_key = :skuKey")
    void updateStockCount(Long skuKey, int newCount);

    @Query("UPDATE stocks SET stock_count = :newCount, updated_at = :timestamp " +
            "WHERE vendor_item_key = :vendorItemKey AND sku_key = :skuKey")
    void updateStockCountAndTimestamp(Long vendorItemKey,Long skuKey, int newCount, LocalDateTime timestamp);



}
