package org.project.ecommerce.common.infrastructure.outbox;

import lombok.RequiredArgsConstructor;
import org.project.ecommerce.common.infrastructure.utils.JsonUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OutboxEventCreator {
    private final OutBoxEventRepository repository;

    public List<OutboxEvent> createStockEvents(EventType eventType,Long skuId,int stockCount,Long vendorItemId) {
        Map<String,Object> payload = new HashMap<>();
        payload.put("skuKey",skuId);
        payload.put("vendorItemKey", vendorItemId);
        payload.put("finalStockCount",stockCount);
        payload.put("timestamp", LocalDateTime.now().toString());

        OutboxEvent event = OutboxEvent.createEvent(eventType.name(), JsonUtils.toJson(payload));
        OutboxEvent savedEvent = repository.save(event);
        return Collections.singletonList(savedEvent);
    }
}
