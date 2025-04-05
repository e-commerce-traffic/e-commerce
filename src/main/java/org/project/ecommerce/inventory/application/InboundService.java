package org.project.ecommerce.inventory.application;

import ch.qos.logback.classic.Logger;
import lombok.RequiredArgsConstructor;
import org.project.ecommerce.category.domain.*;
import org.project.ecommerce.eventstore.outbox.domain.EventType;
import org.project.ecommerce.eventstore.outbox.domain.OutboxEventCreator;
import org.project.ecommerce.inventory.domain.*;
import org.project.ecommerce.inventory.ui.dto.InboundRequestDto;
import org.project.ecommerce.inventory.domain.FcStock;
import org.project.ecommerce.inventory.domain.FcStockId;
import org.project.ecommerce.inventory.domain.FcStockRepository;
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
    private final FcStockRepository fcStockRepository;
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
        FulfillmentCenter fc = getFulfillmentCenter(dto.getFulfillmentCenterId());
        Sku sku = getSku(dto.getSkuId());

        // 입고 기록 저장
        saveInbound(fc.getId(), sku.getId(), dto.getItemCount());

        // 물류센터별 재고 증가
        increaseFcStock(fc, sku, dto.getItemCount());

        createAndPublishEvents(sku, fc,dto.getItemCount());
    }

    private FulfillmentCenter getFulfillmentCenter(Long centerId) {
        return fulfillmentCenterRepository.findById(centerId)
                .orElseThrow(() -> new IllegalStateException("물류센터를 찾을 수 없습니다."));
    }

    private Sku getSku(Long skuId) {
        return skuRepository.findById(skuId)
                .orElseThrow(() -> new IllegalStateException("SKU를 찾을 수 없습니다."));
    }


    private void saveInbound(Long fcId, Long skuId, int itemCount) {
        Inbound inbound = Inbound.create(fcId, skuId, itemCount);
        inboundRepository.save(inbound);
    }

    private void increaseFcStock(FulfillmentCenter fc, Sku sku, int count) {
        FcStockId stockId = FcStockId.of(fc.getId(), sku.getId());
        FcStock stock = fcStockRepository.findById(stockId)
                .orElseGet(() -> FcStock.create(stockId, count));
//        stock.incrementStock(count);
        fcStockRepository.save(stock);
    }


    private void createAndPublishEvents(Sku sku, FulfillmentCenter fc, int stockCount) {
        List<Long> vendorItemIds = vendorItemSkuRepository.findVendorItemIdsBySkuId(sku.getId());

        vendorItemIds.forEach(vendorItemId -> {
            eventCreator.createStockEvents(
                    EventType.INBOUND_CREATED,
                    sku.getId(),
                    stockCount,
                    vendorItemId
            ).forEach(eventPublisher::publishEvent);
        });
    }
}




