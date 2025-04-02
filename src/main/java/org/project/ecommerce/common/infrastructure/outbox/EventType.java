package org.project.ecommerce.common.infrastructure.outbox;

import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public enum EventType {
    ORDER_CREATED("ORDER_CREATED", "order-created"),
    INBOUND_CREATED("INBOUND_CREATED", "inbound-created");

    private final String eventType;
    private final String topic;
    
    // 케이스 구분 없이 이벤트 타입을 빠르게 찾기 위한 맵
    private static final Map<String, String> EVENT_TYPE_TO_TOPIC_MAP = new HashMap<>();
    
    static {
        for (EventType type : values()) {
            EVENT_TYPE_TO_TOPIC_MAP.put(type.eventType.toUpperCase(), type.topic);
        }
    }

    EventType(String eventType, String topic) {
        this.eventType = eventType;
        this.topic = topic;
    }

    /**
     * 이벤트 타입에 해당하는 Kafka 토픽 이름을 반환
     * 대소문자 구분 없이 처리하고 이름 형식 오류에 강건하게 처리
     */
    public static String getTopicByEventType(String eventType) {
        if (eventType == null) {
            throw new IllegalArgumentException("Event type cannot be null");
        }
        
        log.debug("Looking up topic for event type: {}", eventType);
        
        // 1. 대소문자 구분 없이 정확히 매칭되는지 확인
        String normalizedType = eventType.toUpperCase();
        if (EVENT_TYPE_TO_TOPIC_MAP.containsKey(normalizedType)) {
            return EVENT_TYPE_TO_TOPIC_MAP.get(normalizedType);
        }
        
        // 2. 접미사 포함 체크 (예: "order-created-v1" -> "order-created")
        for (Map.Entry<String, String> entry : EVENT_TYPE_TO_TOPIC_MAP.entrySet()) {
            if (normalizedType.contains(entry.getKey())) {
                log.info("Found partial match for event type: {} -> {}", eventType, entry.getValue());
                return entry.getValue();
            }
        }
        
        // 3. 기존 로직 (정확한 매칭)
        String result = Arrays.stream(values())
                .filter(e -> e.eventType.equalsIgnoreCase(eventType))
                .findFirst()
                .map(e -> e.topic)
                .orElse(null);
                
        if (result != null) {
            return result;
        }
        
        // 4. 실패 시 기본값 반환
        log.warn("Unknown event type: {}, defaulting to order-created", eventType);
        return "order-created";
    }
}
