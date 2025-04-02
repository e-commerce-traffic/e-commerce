package org.project.ecommerce.common.infrastructure.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ecommerce.common.infrastructure.utils.JsonUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxEventCreator {
    private final OutBoxEventRepository repository;

    /**
     * 재고 변경 이벤트 생성 - Kafka 파티셔닝 전략을 활용한 동시성 처리 개선
     * 아웃박스 패턴을 통한 트랜잭션 내 이벤트 기록으로 동시성 문제 해결
     * - 타임스탬프 및 고유 이벤트 ID로 이벤트 순서 보장
     * - Kafka 메시지 키를 이용한 파티션 라우팅 보장
     * 
     * @param eventType 이벤트 유형 (ORDER_CREATED 등)
     * @param skuId 재고 SKU ID
     * @param stockCount 최종 재고 수량
     * @param vendorItemId 상품 ID
     * @return 생성된 이벤트 목록
     */
    public List<OutboxEvent> createStockEvents(EventType eventType, Long skuId, int stockCount, Long vendorItemId) {
        String eventId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        
        // 이벤트 페이로드 구성 - Kafka 키 기반 파티셔닝을 위한 정보 추가
        Map<String, Object> payload = new HashMap<>();
        payload.put("skuKey", skuId);
        payload.put("vendorItemKey", vendorItemId);
        payload.put("finalStockCount", stockCount);
        payload.put("timestamp", now.toString());
        payload.put("eventId", eventId);
        
        // Kafka 메시지 키를 저장 (vendorItemKey + skuKey 조합)
        // 동일한 재고에 대한 이벤트는 항상 동일한 파티션으로 라우팅되도록 함
        String messageKey = vendorItemId + ":" + skuId;
        payload.put("messageKey", messageKey);
        
        // 이벤트 생성 및 저장 (아웃박스 패턴)
        OutboxEvent event = OutboxEvent.builder()
                .payload(JsonUtils.toJson(payload))
                .createdAt(now)
                .status(OutboxEvent.OutboxStatus.PENDING)
                .messageKey(messageKey) // 메시지 키 저장 (Kafka 파티션 라우팅용)
                .aggregateType(eventType.name())
                .retryCount(0)
                .build();
                
        OutboxEvent savedEvent = repository.save(event);
        
        log.debug("Stock event created: type={}, skuId={}, count={}, eventId={}, messageKey={}", 
                eventType, skuId, stockCount, eventId, messageKey);
        
        return Collections.singletonList(savedEvent);
    }
}
