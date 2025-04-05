package org.project.ecommerce.category.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.util.List;

public interface VendorItemSkuRepository extends JpaRepository<VendorItemSku, Long> {

    List<VendorItemSku> findAllByIdSkuId(Long skuId);

    @Query("SELECT v.id.vendorItemId FROM VendorItemSku v WHERE v.id.skuId = :skuId")
    List<Long> findVendorItemIdsBySkuId(@Param("skuId") Long skuId);
}
