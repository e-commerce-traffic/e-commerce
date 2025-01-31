package org.project.ecommerce.common.infrastructure.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
        List<OutboxEvent> failedEvents = outboxRepository.findByStatus(OutboxEvent.OutboxStatus.FAILED);

        if (failedEvents.isEmpty()) {
            return;
        }

        log.info("Found {} pending events to retry", failedEvents.size());

        for (OutboxEvent event : failedEvents) {
            try {
                kafkaTemplate.send("order-created", event.getPayload());
                event.markAsCompleted();
                outboxRepository.save(event);
                log.info("Successfully retried event: {}", event.getPayload());
            } catch (Exception e) {
                log.error("Failed to retry event: {}", event.getPayload(), e);
                throw new RuntimeException("Failed to retry event", e);
//                event.markAsFailed();
//                outboxRepository.save(event);
            }
        }
    }
}
