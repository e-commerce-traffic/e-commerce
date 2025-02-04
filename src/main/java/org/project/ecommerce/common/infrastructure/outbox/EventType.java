package org.project.ecommerce.common.infrastructure.outbox;

import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
@Slf4j
public enum EventType {
    ORDER_CREATED("ORDER_CREATED", "order-created"),
    INBOUND_CREATED("INBOUND_CREATED", "inbound-created");

    private final String eventType;
    private final String topic;

    EventType(String eventType, String topic) {
        this.eventType = eventType;
        this.topic = topic;
    }

    public static String getTopicByEventType(String eventType) {
        log.info("getTopicByEventType: {}", eventType);
        return Arrays.stream(values())
                .filter(e -> e.eventType.equals(eventType))
                .findFirst()
                .map(e -> e.topic)
                .orElseThrow(() -> new IllegalArgumentException("Unknown event type: " + eventType));
    }
}
