package org.project.ecommerce.fulfillment.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InboundRepository extends JpaRepository<Inbound,Long> {

    @Query("SELECT COALESCE(SUM(i.itemCount),0) FROM Inbound i WHERE i.sku.id = :skuId")
    int suItemCountBySku(@Param("skuId") Long skuId);

    List<Inbound> findBySkuId(Long skuId);
}
