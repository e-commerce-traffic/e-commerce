package org.project.ecommerce.common.infastructure.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ecommerce.order.domain.OutBoxEventRepository;
import org.project.ecommerce.order.domain.OutboxEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventHandler {
    private final OutBoxEventRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void handleOrderCreated(OutboxEvent event) {
        try {
            kafkaTemplate.send("order-created", event.getPayload());
            event.markAsPublished();
            outboxRepository.save(event);
        } catch (Exception e) {
            log.error("Failed to process event: {}", event.getPayload(), e);
            event.markAsFailed();
            outboxRepository.save(event);
        }
    }
}
