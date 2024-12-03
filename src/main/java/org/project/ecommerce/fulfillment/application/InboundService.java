package org.project.ecommerce.fulfillment.application;

import lombok.RequiredArgsConstructor;
import org.project.ecommerce.fulfillment.domain.FulfillmentCenter;
import org.project.ecommerce.fulfillment.domain.FulfillmentCenterRepository;
import org.project.ecommerce.fulfillment.domain.Inbound;
import org.project.ecommerce.fulfillment.domain.InboundRepository;
import org.project.ecommerce.fulfillment.ui.dto.InboundRequestDto;
import org.project.ecommerce.fulfillment.domain.Sku;
import org.project.ecommerce.fulfillment.domain.SkuRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InboundService {

    private final InboundRepository inboundRepository;
    private final FulfillmentCenterRepository fulfillmentCenterRepository;
    private final SkuRepository skuRepository;


    public int getStockByVendorItem(Long vendorItemId) {
        List<Sku> skus = skuRepository.findByVendorItemId(vendorItemId);

        if(skus.isEmpty()){
            throw new IllegalArgumentException("Invalid VendorItem");
        }
        return skus.stream().mapToInt(sku -> inboundRepository.suItemCountBySku(sku.getId())).sum();
    }

    public ResponseEntity<Void> createStock(InboundRequestDto dto) {

        FulfillmentCenter fulfillmentCenter = fulfillmentCenterRepository.findById(dto.getFulfillmentCenterId())
                .orElseThrow(() -> new IllegalArgumentException("Fulfilment center not found"));
        Sku sku = skuRepository.findById(dto.getSkuId())
                .orElseThrow(() -> new IllegalArgumentException("Sku not found"));

        Inbound inbound = Inbound.create(sku, fulfillmentCenter, dto.getItemCount());
        inboundRepository.save(inbound);

        return null;
    }


}
