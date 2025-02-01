package org.project.ecommerce.common.infrastructure.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ecommerce.common.infrastructure.outbox.OutBoxEventRepository;
import org.project.ecommerce.common.infrastructure.utils.JsonUtils;
import org.project.ecommerce.fulfillment.domain.Stock;
import org.project.ecommerce.fulfillment.domain.StockRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockEventConsumer {
    private final StockRepository stockRepository;


    //    @Transactional
    @KafkaListener(topics = {"order-created", "inbound-created"}, groupId = "stock-consumer-group")
    public void handleOrderCreated(String eventPayload) {
        try {
            JsonNode event = JsonUtils.parse(eventPayload);
            Long skuKey = event.get("skuKey").asLong();
            Long vendorItemKey = event.get("vendorItemKey").asLong();
            int finalStockCount = event.get("finalStockCount").asInt();
            LocalDateTime eventTimestamp = LocalDateTime.parse(event.get("timestamp").asText());

            log.info("Received stock event - vendorItemKey: {}, skuKey: {}, count: {}, timestamp: {}",
                    vendorItemKey, skuKey, finalStockCount, eventTimestamp);

            Stock currentStock = stockRepository.findBySkuKeyAndVendorItemKey(skuKey, vendorItemKey);

            ZonedDateTime currentStockTime = currentStock.getUpdatedAt().atZone(ZoneOffset.UTC);
            ZonedDateTime eventTime = eventTimestamp.atZone(ZoneOffset.UTC);


            if (currentStock == null || currentStockTime.isBefore(eventTime)) {
                stockRepository.updateStockCountAndTimestamp(
                        vendorItemKey,
                        skuKey,
                        finalStockCount,
                        eventTimestamp
                );
                log.info("Stock updated - vendorItemKey: {}, skuKey: {}, newCount: {}",
                        vendorItemKey, skuKey, finalStockCount);
            } else {
                log.info("Update skipped - vendorItemKey: {}, skuKey: {}, oldTimestamp: {}, newTimestamp: {}",
                        vendorItemKey, skuKey, currentStock.getUpdatedAt(), eventTimestamp);
            }
        } catch (IllegalStateException e) {  // 재고 부족
            log.error("Business logic failure: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Technical failure: {}", e.getMessage());
            throw e;
        }
    }
}