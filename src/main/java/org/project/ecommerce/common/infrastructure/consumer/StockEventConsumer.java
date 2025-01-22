package org.project.ecommerce.common.infrastructure.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ecommerce.common.infrastructure.outbox.OutBoxEventRepository;
import org.project.ecommerce.common.infrastructure.outbox.OutboxEvent;
import org.project.ecommerce.common.infrastructure.utils.JsonUtils;
import org.project.ecommerce.fulfillment.domain.StockRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

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
            Long outboxId = event.get("outboxId").asLong();
            Long skuKey = event.get("skuKey").asLong();
            int decreasedCount = event.get("decreasedCount").asInt();


            // 이벤트 조회

            OutboxEvent outboxEvent = outboxRepository.findById(outboxId)
                    .orElseThrow(() -> new IllegalArgumentException("Event not found: " + outboxId));

            if (outboxEvent.getStatus() == OutboxEvent.OutboxStatus.COMPLETED) {
                log.info("Event already processed: {}", outboxId);
                return;
            }

            // PENDING 상태가 아니면 처리하지 않음
//            if (outboxEvent.getStatus() != OutboxEvent.OutboxStatus.PENDING) {
//                log.warn("Event in invalid status: {} - {}", outboxId, outboxEvent.getStatus());
//                return;
//            }
//

            // 재고 확인
            Integer currentStock = stockRepository.findStockCountBySkuKey(skuKey);
            if (currentStock == null || currentStock < decreasedCount) {
                outboxEvent.markAsFailed();
                outboxRepository.save(outboxEvent);
                throw new IllegalStateException("Insufficient stock for skuKey: " + skuKey);
            }

            // 재고 차감 시도
            boolean updated = stockRepository.updateStockCount(skuKey, currentStock - decreasedCount, currentStock);
            if (!updated) {
                outboxEvent.markAsFailed();
                outboxRepository.save(outboxEvent);
                throw new IllegalStateException("Concurrent modification detected");
            }

            // 성공적으로 처리됨을 표시
            outboxEvent.markAsCompleted();
            outboxRepository.save(outboxEvent);

            log.info("Successfully processed event: {}", outboxId);

        } catch (IllegalStateException e) {  // 재고 부족
            log.error("Business logic failure: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Technical failure: {}", e.getMessage());
            throw e;
        }
    }
}