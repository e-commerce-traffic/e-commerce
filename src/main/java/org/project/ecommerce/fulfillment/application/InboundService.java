package org.project.ecommerce.fulfillment.application;

import lombok.RequiredArgsConstructor;
import org.project.ecommerce.fulfillment.domain.FulfillmentCenter;
import org.project.ecommerce.fulfillment.domain.FulfillmentCenterRepository;
import org.project.ecommerce.fulfillment.domain.Inbound;
import org.project.ecommerce.fulfillment.domain.InboundRepository;
import org.project.ecommerce.fulfillment.ui.dto.InboundRequestDto;
import org.project.ecommerce.order.domain.Sku;
import org.project.ecommerce.order.domain.SkuRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InboundService {

    private final InboundRepository repository;
    private final FulfillmentCenterRepository fulfillmentCenterRepository;
    private final SkuRepository skuRepository;


    public ResponseEntity<Void> createStock(InboundRequestDto dto) {

        FulfillmentCenter fulfillmentCenter = fulfillmentCenterRepository.findById(dto.getFulfillmentCenterId())
                .orElseThrow(() -> new IllegalArgumentException("Fulfilment center not found"));
        Sku sku = skuRepository.findById(dto.getSkuId())
                .orElseThrow(() -> new IllegalArgumentException("Sku not found"));

        Inbound inbound = Inbound.create(sku, fulfillmentCenter, dto.getItemCount());
        repository.save(inbound);

        return null;
    }

}
