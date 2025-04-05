package org.project.ecommerce.order.domain;

import org.project.ecommerce.category.domain.VendorItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorItemRepository extends JpaRepository<VendorItem,Long> {
}
