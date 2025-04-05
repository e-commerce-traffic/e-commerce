package org.project.ecommerce.inventory.domain;

import org.springframework.data.jpa.repository.JpaRepository;


import java.util.Optional;

public interface FcStockRepository extends JpaRepository<FcStock, FcStockId> {

    Optional<FcStock> findById(FcStockId id);
}
