package org.project.ecommerce.inventory.domain;

import org.project.ecommerce.category.domain.Sku;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InboundRepository extends JpaRepository<Inbound,Long> {


}
