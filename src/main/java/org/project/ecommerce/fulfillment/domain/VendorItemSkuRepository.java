package org.project.ecommerce.fulfillment.domain;

import org.project.ecommerce.order.domain.VendorItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VendorItemSkuRepository extends JpaRepository<VendorItemSku,Long> {
    List<VendorItemSku> findByVendorItem_Id(Long vendorItemId);

    @Query("SELECT vis.vendorItem.id FROM VendorItemSku vis WHERE vis.sku.id = :skuId")
    List<Long> findVendorItemIdsBySkuId(@Param("skuId") Long skuId);

}
