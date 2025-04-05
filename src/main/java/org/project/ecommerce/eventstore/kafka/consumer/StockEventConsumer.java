package org.project.ecommerce.eventstore.kafka.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ecommerce.infrastructure.utils.JsonUtils;
import org.project.ecommerce.inventory.domain.Stock;
import org.project.ecommerce.inventory.domain.StockKey;
import org.project.ecommerce.inventory.domain.StockRepository;
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
            StockEvent event = parseAndValidate(eventPayload);

            boolean updated = processStock(event);

            acknowledgment.acknowledge();
            log.info("Event processed - eventId={}, updated={}", event.eventId(), updated);

        } catch (Exception e) {
            log.error("Failed to process event: {}", e.getMessage(), e);
            throw e;
        }
    }

    private StockEvent parseAndValidate(String payload) {
        JsonNode node = JsonUtils.parse(payload);
        Long skuKey = node.get("skuKey").asLong();
        Long vendorItemKey = node.get("vendorItemKey").asLong();
        int delta = node.get("delta").asInt();
        String eventId = node.get("eventId").asText();
        LocalDateTime timestamp = LocalDateTime.parse(node.get("timestamp").asText());

        if (delta == 0) throw new IllegalArgumentException("delta must not be zero");

        return new StockEvent(eventId, skuKey, vendorItemKey, delta, timestamp);
    }

    private boolean processStock(StockEvent event) {
        StockKey stockKey = new StockKey(event.vendorItemKey(), event.skuKey());

        Stock current = stockRepository.findBySkuKeyAndVendorItemKey(
                event.skuKey(), event.vendorItemKey());

        if (current == null) {
            Stock newStock = Stock.create(stockKey, event.delta(), event.timestamp());
            stockRepository.save(newStock);
            log.info("Stock created - vendorItemKey={}, skuKey={}, count={}",
                    stockKey.getVendorItemKey(), stockKey.getSkuKey(), event.delta());
            return true;
        }

        if (current.getUpdatedAt().isAfter(event.timestamp())) {
            log.info("Skipped outdated event - eventId={}, stockUpdatedAt={}",
                    event.eventId(), current.getUpdatedAt());
            return false;
        }

        current.applyDelta(event.delta(), event.timestamp());
        stockRepository.save(current);

        log.info("Stock updated - vendorItemKey={}, skuKey={}, newCount={}",
                stockKey.getVendorItemKey(), stockKey.getSkuKey(), current.getStockCount());

        return true;
    }


    private record StockEvent(String eventId, Long skuKey, Long vendorItemKey, int delta, LocalDateTime timestamp) {}
}