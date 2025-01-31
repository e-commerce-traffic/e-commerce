package org.project.ecommerce.common.infrastructure.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OutBoxEventRepository extends JpaRepository<OutboxEvent, Long> {
    Optional<OutboxEvent> findByIdAndStatus(Long id, OutboxEvent.OutboxStatus status);

    List<OutboxEvent> findByStatus(OutboxEvent.OutboxStatus status);

    @Modifying
    @Query("UPDATE OutboxEvent e SET e.status = :newStatus WHERE e.id = :id AND e.status = :expectedStatus")
    boolean updateStatus(@Param("id") Long id,
                         @Param("expectedStatus") String expectedStatus,
                         @Param("newStatus") String newStatus);
}
