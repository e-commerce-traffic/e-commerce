package org.project.ecommerce.common.infrastructure.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ecommerce.common.infrastructure.utils.JsonUtils;
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
            JsonNode payload = JsonUtils.parse(event.getPayload());
            Long skuKey = payload.get("skuKey").asLong();
            int finalStockCount = payload.get("finalStockCount").asInt();

            log.info("Sending event for skuKey: {}, finalStockCount: {}", skuKey, finalStockCount);

            kafkaTemplate.send("order-created", skuKey.toString(), event.getPayload());
            event.markAsCompleted();
            outboxRepository.save(event);
        } catch (Exception e) {
            log.error("Failed to process event: {}", event.getPayload(), e);
            event.markAsFailed();
            outboxRepository.save(event);
        }
    }
}
