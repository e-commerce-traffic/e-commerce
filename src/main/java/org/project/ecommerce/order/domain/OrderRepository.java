package org.project.ecommerce.order.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order,Long> {
    boolean existsByIdempotencyKey(String idempotencyKey);

}
