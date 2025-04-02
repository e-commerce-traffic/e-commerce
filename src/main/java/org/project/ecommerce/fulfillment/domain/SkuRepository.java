package org.project.ecommerce.fulfillment.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SkuRepository extends JpaRepository<Sku, Long> {
    @Modifying
    @Query("update Sku s set s.stockCount = s.stockCount - :count where s.id = :skuId and s.stockCount >= :count")
    int decreaseStock(@Param("skuId") Long skuId, @Param("count") int count);

}
