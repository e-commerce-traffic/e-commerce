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
    
    private static final int MAX_RETRY_COUNT = 3;

    /**
     * 메시지 키를 활용한 순서 보장 및 처리 방식으로 개선된 아웃박스 이벤트 처리기
     * 1. 각 이벤트마다 재시도 카운트 관리
     * 2. 메시지 키를 사용하여 동일 엔티티에 대한 이벤트는 동일 파티션으로 라우팅
     * 3. 실패한 이벤트 주기적 재처리
     */
    @Scheduled(fixedDelayString = "${outbox.processor.interval:300000}")
    @Transactional
    public void processOutboxEvents() {
        List<OutboxEvent> pendingEvents = outboxRepository.findByStatus(OutboxEvent.OutboxStatus.PENDING);

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.info("Found {} pending events to process", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            try {
                event.incrementRetryCount();
                
                // 최대 재시도 횟수 초과 시 실패 처리
                if (event.getRetryCount() > MAX_RETRY_COUNT) {
                    log.warn("Event exceeded max retry count: id={}, type={}, retries={}", 
                            event.getId(), event.getAggregateType(), event.getRetryCount());
                    event.markAsFailed();
                    outboxRepository.save(event);
                    continue;
                }
                
                // 토픽 결정
                String topic = EventType.getTopicByEventType(event.getAggregateType());
                
                // 메시지 키 추출 (파티션 결정에 사용)
                String messageKey = event.getMessageKey();
                if (messageKey == null) {
                    // 기존 이벤트 호환성 유지 - 페이로드에서 키를 추출
                    JsonNode payload = JsonUtils.parse(event.getPayload());
                    if (payload.has("messageKey")) {
                        messageKey = payload.get("messageKey").asText();
                    } else if (payload.has("skuKey") && payload.has("vendorItemKey")) {
                        messageKey = payload.get("vendorItemKey").asText() + ":" + payload.get("skuKey").asText();
                    } else {
                        // 키가 없는 경우 랜덤 파티션에 할당됨
                        messageKey = event.getId().toString();
                    }
                }

                // Kafka로 메시지 전송 - 메시지 키를 사용하여 동일 키는 동일 파티션으로 라우팅
                kafkaTemplate.send(topic, messageKey, event.getPayload());
                
                // 성공 처리 및 상태 업데이트
                event.markAsCompleted();
                outboxRepository.save(event);

                log.info("Successfully processed event: id={}, type={}, key={}, topic={}", 
                        event.getId(), event.getAggregateType(), messageKey, topic);
                
            } catch (Exception e) {
                log.error("Failed to process event: id={}, type={}, retry={}", 
                        event.getId(), event.getAggregateType(), event.getRetryCount(), e);
                
                // 실패 상태로 마킹하지만 삭제하지 않음 (다음 스케줄에서 재시도)
                outboxRepository.save(event);
            }
        }
    }
    
    /**
     * 실패한 이벤트 재처리 (별도 스케줄로 운영)
     */
    @Scheduled(fixedDelayString = "${outbox.processor.failed-retry-interval:300000}")
    @Transactional
    public void retryFailedEvents() {
        List<OutboxEvent> failedEvents = outboxRepository.findByStatus(OutboxEvent.OutboxStatus.FAILED);
        
        if (failedEvents.isEmpty()) {
            return;
        }
        
        log.info("Found {} failed events to retry", failedEvents.size());
        
        for (OutboxEvent event : failedEvents) {
            // 재시도 횟수가 너무 많은 이벤트는 스킵
            if (event.getRetryCount() > MAX_RETRY_COUNT * 2) {
                log.warn("Skipping event with excessive retry count: id={}, type={}, retries={}", 
                        event.getId(), event.getAggregateType(), event.getRetryCount());
                continue;
            }
            
            // 상태를 PENDING으로 변경하여 다음 processOutboxEvents 호출에서 처리되도록 함
            event.incrementRetryCount();
            event.markAsPending();
            outboxRepository.save(event);
            
            log.info("Marked failed event for retry: id={}, type={}, retries={}", 
                    event.getId(), event.getAggregateType(), event.getRetryCount());
        }
    }
}
