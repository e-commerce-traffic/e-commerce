package org.project.ecommerce.common.infrastructure.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ecommerce.common.infrastructure.utils.JsonUtils;
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
                String topic = EventType.getTopicByEventType(event.getEventType());
                JsonNode payload = JsonUtils.parse(event.getPayload());
                String skuKey = payload.get("skuKey").asText();

                kafkaTemplate.send(topic, skuKey, event.getPayload());
                event.markAsCompleted();
                outboxRepository.save(event);

                log.info("Successfully retried event: {} for topic: {}", event.getPayload(),topic);
            } catch (Exception e) {
                log.error("Failed to retry event: {} with type: {}",
                        event.getPayload(), event.getEventType(), e);
                throw new RuntimeException("Failed to retry event", e);
//                event.markAsFailed();
//                outboxRepository.save(event);
            }
        }
    }
}
