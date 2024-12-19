package org.project.ecommerce.fulfillment.ui;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.project.ecommerce.fulfillment.application.InboundService;
import org.project.ecommerce.fulfillment.domain.Sku;
import org.project.ecommerce.fulfillment.ui.dto.InboundRequestDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/stock")
@RequiredArgsConstructor
public class FulfillmentController {
    private final InboundService service;


    @GetMapping("/{vendorItemId}")
    public ResponseEntity<Integer> getStock(@PathVariable Long vendorItemId) {
        // 총 재고 수량 반환
        int totalStock = service.getTotalStockByVendorItem(vendorItemId);
        return ResponseEntity.ok(totalStock);
    }


    /**
     * 입고 api
     * @param dto {fulfillmentCenterId,skuId,itemCount}
     *
     * @return void
     */
    @PostMapping("/inbound")
    public ResponseEntity<Void> createInbound(@RequestBody @Valid InboundRequestDto dto) {

         service.createStock(dto);
         return ResponseEntity.ok().build();
    }
}
