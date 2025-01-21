package org.project.ecommerce.common.infastructure.consumer;

import lombok.RequiredArgsConstructor;
import org.project.ecommerce.order.domain.OutBoxEventRepository;
import org.project.ecommerce.order.domain.OutboxEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OutboxEventProcessor {
    private final OutBoxEventRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
//
//    @Scheduled(fixedDelay = 1000)
//    @Transactional
//    public void processOutboxEvents() {
//        List<OutboxEvent> pendingEvents = outboxRepository.findByStatus("PENDING");
//
//        for (OutboxEvent event : pendingEvents) {
//            try {
//                kafkaTemplate.send("order-created", event.getPayload());
//                event.markAsPublished();
//                outboxRepository.save(event);
//            } catch (Exception e) {
//                event.markAsFailed();
//                outboxRepository.save(event);
//            }
//        }
//    }

}
