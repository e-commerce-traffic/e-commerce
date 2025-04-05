package org.project.ecommerce.eventstore.outbox.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "outbox_events")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "outbox_id")
    private Long id;

    @Column(name = "aggregate_type", nullable = false, length = 50)
    private String aggregateType;

    @Column(columnDefinition = "json", nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus status;

    @Column(name = "message_key", length = 255)
    private String messageKey;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "retry_count")
    private Integer retryCount;

    /**
     * 레거시 지원을 위한 팩토리 메서드
     */
    public static OutboxEvent createEvent(String eventType, String payload) {
        return OutboxEvent.builder()
                .aggregateType(eventType)
                .payload(payload)
                .status(OutboxStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .retryCount(0)
                .build();
    }

    public enum OutboxStatus {
        PENDING, PROCESSING, COMPLETED, FAILED
    }

    public void markAsCompleted() {
        this.status = OutboxStatus.COMPLETED;
        this.publishedAt = LocalDateTime.now();
    }

    public void markAsFailed() {
        this.status = OutboxStatus.FAILED;
        this.publishedAt = LocalDateTime.now();
    }

    public void markAsPending() {
        this.status = OutboxStatus.PENDING;
        this.publishedAt = LocalDateTime.now();
    }

    public void incrementRetryCount() {
        if (this.retryCount == null) {
            this.retryCount = 0;
        }
        this.retryCount++;
    }
}
