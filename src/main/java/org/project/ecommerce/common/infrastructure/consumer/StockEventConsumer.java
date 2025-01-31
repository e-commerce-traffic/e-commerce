package org.project.ecommerce.common.infrastructure.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ecommerce.common.infrastructure.outbox.OutBoxEventRepository;
import org.project.ecommerce.common.infrastructure.outbox.OutboxEvent;
import org.project.ecommerce.common.infrastructure.utils.JsonUtils;
import org.project.ecommerce.fulfillment.domain.Stock;
import org.project.ecommerce.fulfillment.domain.StockRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockEventConsumer {
    private final StockRepository stockRepository;
    private final OutBoxEventRepository outboxRepository;


    //    @Transactional
    @KafkaListener(topics = "order-created", groupId = "stock-consumer-group")
    public void handleOrderCreated(String eventPayload) {
        try {
            JsonNode event = JsonUtils.parse(eventPayload);
            Long skuKey = event.get("skuKey").asLong();
            int finalStockCount = event.get("finalStockCount").asInt();
            LocalDateTime eventTimestamp = LocalDateTime.parse(event.get("timestamp").asText());

            Stock currentStock = stockRepository.findBySkuKey(skuKey);

            // 시간 비교를 좀 더 관대하게
            if (Duration.between(currentStock.getUpdatedAt(), eventTimestamp).toSeconds() >= 0) {
                stockRepository.updateStockCountAndTimestamp(skuKey, finalStockCount, eventTimestamp);
                log.info("Stock updated - skuKey: {}, newCount: {}", skuKey, finalStockCount);
            } else {
                log.info("Update skipped - skuKey: {}, oldTimestamp: {}, newTimestamp: {}",
                        skuKey, currentStock.getUpdatedAt(), eventTimestamp);
            }        } catch (IllegalStateException e) {  // 재고 부족
            log.error("Business logic failure: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Technical failure: {}", e.getMessage());
            throw e;
        }
    }
}