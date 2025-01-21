package org.project.ecommerce.common.infastructure.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ecommerce.order.domain.OutBoxEventRepository;
import org.project.ecommerce.order.domain.OutboxEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxEventProcessor {
    private final OutBoxEventRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void processOutboxEvents() {
        List<OutboxEvent> pendingEvents = outboxRepository.findByStatus("PENDING");

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.info("Found {} pending events to retry", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            try {
                kafkaTemplate.send("order-created", event.getPayload());
                event.markAsPublished();
                outboxRepository.save(event);
                log.info("Successfully retried event: {}", event.getPayload());
            } catch (Exception e) {
                log.error("Failed to retry event: {}", event.getPayload(), e);
                event.markAsFailed();
                outboxRepository.save(event);
            }
        }
    }
}
