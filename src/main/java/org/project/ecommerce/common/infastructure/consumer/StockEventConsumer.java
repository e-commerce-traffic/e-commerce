package org.project.ecommerce.common.infastructure.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ecommerce.common.utils.JsonUtils;
import org.project.ecommerce.fulfillment.domain.StockRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockEventConsumer {
    private final StockRepository stockRepository;

    @KafkaListener(topics = "order-created", groupId = "stock-consumer-group")
    public void handleOrderCreated(String eventPayload) {
        try {
            JsonNode event = JsonUtils.parse(eventPayload);
            Long skuKey = event.get("skuKey").asLong();
            int decreasedCount = event.get("decreasedCount").asInt();

            Integer currentStock = stockRepository.findStockCountBySkuKey(skuKey);
            if (currentStock != null) {
                int newStock = currentStock - decreasedCount;
                if (newStock >= 0) {
                    stockRepository.updateStockCount(skuKey, newStock);
                } else {
                    log.error("Stock would become negative for skuKey: {}", skuKey);
                }
            }
        } catch (Exception e) {
            log.error("Failed to process order-created event: {}", eventPayload, e);
        }
    }
}
