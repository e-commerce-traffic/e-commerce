package org.project.ecommerce.fulfillment.domain;

import org.project.ecommerce.order.domain.VendorItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VendorItemSkuRepository extends JpaRepository<VendorItemSku,Long> {
    List<VendorItemSku> findByVendorItem_Id(Long vendorItemId);
}
