package org.project.ecommerce.fulfillment.application;

import ch.qos.logback.classic.Logger;
import lombok.RequiredArgsConstructor;
import org.project.ecommerce.common.infrastructure.outbox.EventType;
import org.project.ecommerce.common.infrastructure.outbox.OutboxEvent;
import org.project.ecommerce.common.infrastructure.outbox.OutboxEventCreator;
import org.project.ecommerce.fulfillment.domain.*;
import org.project.ecommerce.fulfillment.ui.dto.InboundRequestDto;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InboundService {

    private final InboundRepository inboundRepository;
    private final FulfillmentCenterRepository fulfillmentCenterRepository;
    private final SkuRepository skuRepository;
    private final VendorItemSkuRepository vendorItemSkuRepository;
    private final OutboxEventCreator eventCreator;
    private final ApplicationEventPublisher eventPublisher;

    private final Logger log = (Logger) LoggerFactory.getLogger(getClass());

    /**
     * @param dto {fulfillmentCenterId,skuId,itemCount}
     */

    @Transactional
    public void createInbound(InboundRequestDto dto) {
        FulfillmentCenter center = getFulfillmentCenter(dto.getFulfillmentCenterId());
        Sku sku = getSku(dto.getSkuId());

        saveInbound(center, sku, dto.getItemCount());
        updateSkuStock(sku, dto.getItemCount());
        createAndPublishEvents(sku);
    }

    private FulfillmentCenter getFulfillmentCenter(Long centerId) {
        return fulfillmentCenterRepository.findById(centerId)
                .orElseThrow(() -> new IllegalStateException("물류센터를 찾을 수 없습니다."));
    }

    private Sku getSku(Long skuId) {
        return skuRepository.findById(skuId)
                .orElseThrow(() -> new IllegalStateException("SKU를 찾을 수 없습니다."));
    }

    private void saveInbound(FulfillmentCenter center, Sku sku, int itemCount) {
        Inbound inbound = Inbound.create(center, sku, itemCount);
        inboundRepository.save(inbound);
    }

    private void updateSkuStock(Sku sku, int itemCount) {
        sku.incrementStock(itemCount);
        skuRepository.save(sku);
    }

    private void createAndPublishEvents(Sku sku) {
        List<Long> vendorItemIds = vendorItemSkuRepository.findVendorItemIdsBySkuId(sku.getId());
        vendorItemIds.forEach(vendorItemId -> {
            eventCreator.createStockEvents(
                    EventType.INBOUND_CREATED,
                    sku.getId(),
                    sku.getStockCount(),
                    vendorItemId
            ).forEach(eventPublisher::publishEvent);
        });
    }
}




