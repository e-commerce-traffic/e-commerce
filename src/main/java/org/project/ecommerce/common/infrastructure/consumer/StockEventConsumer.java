package org.project.ecommerce.common.infrastructure.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ecommerce.common.infrastructure.utils.JsonUtils;
import org.project.ecommerce.fulfillment.domain.Stock;
import org.project.ecommerce.fulfillment.domain.StockRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockEventConsumer {
    private final StockRepository stockRepository;
    
    /**
     * 주문 및 입고 이벤트 처리 - Kafka 파티션 기반 동시성 처리
     * 1. Kafka 컨슈머 그룹과 오프셋 관리를 통한 중복 방지
     * 2. 메시지 키 기반 파티셔닝으로 동일 재고 관련 이벤트는 항상 순서 보장
     * 3. 명시적 승인(ACK)을 통한 정확한 메시지 처리 보장
     * 4. 타임스탬프 기반 충돌 해결
     */
    @KafkaListener(topics = {"order-created", "inbound-created"}, groupId = "stock-consumer-group")
    public void handleStockEvent(
            @Payload String eventPayload,
            @Header(KafkaHeaders.RECEIVED_KEY) String messageKey,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        
        try {
            JsonNode event = JsonUtils.parse(eventPayload);
            
            // 필수 필드 추출
            Long skuKey = event.get("skuKey").asLong();
            Long vendorItemKey = event.get("vendorItemKey").asLong();
            int finalStockCount = event.get("finalStockCount").asInt();
            LocalDateTime eventTimestamp = LocalDateTime.parse(event.get("timestamp").asText());
            String eventId = event.has("eventId") ? event.get("eventId").asText() : null;
            
            log.info("Processing stock event - topic: {}, partition: {}, offset: {}, key: {}, eventId: {}",
                    topic, partition, offset, messageKey, eventId);
            
            // 재고 조회는 언제나 최신값을 반환하므로 현재 DB 상태를 확인
            Stock currentStock = stockRepository.findBySkuKeyAndVendorItemKey(skuKey, vendorItemKey);
            
            // 이벤트 타임스탬프 기반 동시성 처리 - 최신 이벤트만 적용
            boolean updated = false;
            if (currentStock == null) {
                // 첫 생성인 경우
                stockRepository.updateStockCountAndTimestamp(
                        vendorItemKey, skuKey, finalStockCount, eventTimestamp);
                log.info("Stock created - vendorItemKey: {}, skuKey: {}, count: {}", 
                        vendorItemKey, skuKey, finalStockCount);
                updated = true;
            } else {
                ZonedDateTime currentStockTime = currentStock.getUpdatedAt().atZone(ZoneOffset.UTC);
                ZonedDateTime eventTime = eventTimestamp.atZone(ZoneOffset.UTC);
                
                if (currentStockTime.isBefore(eventTime)) {
                    // 이벤트가 더 최신인 경우만 업데이트
                    stockRepository.updateStockCountAndTimestamp(
                            vendorItemKey, skuKey, finalStockCount, eventTimestamp);
                    log.info("Stock updated - vendorItemKey: {}, skuKey: {}, newCount: {}", 
                            vendorItemKey, skuKey, finalStockCount);
                    updated = true;
                } else {
                    // 더 오래된 이벤트는 무시 (순서 보장 처리)
                    log.info("Update skipped (out-of-order event) - key: {}, stockTime: {}, eventTime: {}",
                            messageKey, currentStock.getUpdatedAt(), eventTimestamp);
                }
            }
            
            // 모든 처리가 완료되면 명시적으로 승인 (오프셋 커밋)
            acknowledgment.acknowledge();
            
            log.info("Event processed and acknowledged - topic: {}, partition: {}, offset: {}, updated: {}", 
                    topic, partition, offset, updated);
            
        } catch (Exception e) {
            log.error("Failed to process stock event: topic={}, partition={}, offset={}, error={}",
                    topic, partition, offset, e.getMessage(), e);
            
            // 예외 발생 시 처리 실패로 간주하고 메시지를 다시 처리하도록 승인하지 않음
            // Kafka의 자동 재시도 메커니즘 활용 (설정에 따라 다름)
            throw e;
        }
    }
}