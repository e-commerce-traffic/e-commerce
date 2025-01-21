package org.project.ecommerce.order.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.project.ecommerce.common.utils.JsonUtils;
import org.project.ecommerce.order.ui.dto.OrderCreatedEvent;

import java.time.LocalDateTime;

@Entity
@Table(name = "outbox_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "outbox_id")
    private Long id;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(columnDefinition = "json", nullable = false)
    private String payload;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Builder
    public OutboxEvent(String eventType, String payload) {
        this.eventType = eventType;
        this.payload = payload;
        this.status = status != null ? status : "PENDING";
        this.createdAt = LocalDateTime.now();
    }

    public static OutboxEvent createEvent(String eventType, String payload) {
        return OutboxEvent.builder()
                .eventType(eventType)
                .payload(payload)
                .build();
    }

    public void markAsPublished() {
        this.status = "PUBLISHED";
        this.publishedAt = LocalDateTime.now();
    }

    public void markAsFailed() {
        this.status = "FAILED";
    }

}
