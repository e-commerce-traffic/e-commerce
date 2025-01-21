package org.project.ecommerce.order.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutBoxEventRepository extends JpaRepository<OutboxEvent,Long> {
    List<OutboxEvent> findByStatusOrderByCreatedAt(String status);

    List<OutboxEvent> findByStatus(String pending);
}
