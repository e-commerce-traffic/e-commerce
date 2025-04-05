package org.project.ecommerce.eventstore.outbox.domain;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ecommerce.infrastructure.utils.JsonUtils;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutBoxEventHandler {
    private final OutBoxEventRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void handleOrderCreated(OutboxEvent event) {
        try {
            String topic = determineTopicByEventType(event.getAggregateType());
            JsonNode payload = JsonUtils.parse(event.getPayload());
            
            // 메시지 키 추출 (파티션 라우팅용)
            String messageKey = event.getMessageKey();
            if (messageKey == null) {
                // 기존 이벤트 호환성 유지
                if (payload.has("messageKey")) {
                    messageKey = payload.get("messageKey").asText();
                } else if (payload.has("skuKey") && payload.has("vendorItemKey")) {
                    messageKey = payload.get("vendorItemKey").asText() + ":" + payload.get("skuKey").asText();
                } else {
                    messageKey = event.getId().toString();
                }
            }
            
            log.info("Sending event to topic: {}, key: {}, id: {}", topic, messageKey, event.getId());

            kafkaTemplate.send(topic, messageKey, event.getPayload())
                    .whenComplete((result, ex) -> {
                        if (null == ex) {
                            event.markAsCompleted();
                            log.info("Event sent successfully to topic: {}, partition: {}, offset: {}", 
                                    topic, result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                        } else {
                            event.markAsFailed();
                            log.error("Failed to send event: {}", ex.getMessage());
                        }
                        outboxRepository.save(event);
                    });

        } catch (Exception e) {
            log.error("Failed to process event: id={}, payload={}", event.getId(), event.getPayload(), e);
            event.markAsFailed();
            outboxRepository.save(event);
        }
    }

    private String determineTopicByEventType(String eventType) {
        return EventType.getTopicByEventType(eventType);
    }
}
