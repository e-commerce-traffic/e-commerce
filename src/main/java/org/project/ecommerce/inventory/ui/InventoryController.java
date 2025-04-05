package org.project.ecommerce.inventory.ui;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.project.ecommerce.inventory.application.InboundService;
import org.project.ecommerce.inventory.application.StockService;
import org.project.ecommerce.inventory.ui.dto.InboundRequestDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/stock")
@RequiredArgsConstructor
public class InventoryController {
    private final InboundService inboundService;
    private final StockService stockService;


    @GetMapping("/{vendorItemId}")
    public ResponseEntity<Integer> getStock(@PathVariable Long vendorItemId) {
        int stockCount = stockService.getStockByVendorItemId(vendorItemId);
        return ResponseEntity.ok(stockCount);
    }


    /**
     * 입고 api
     * @param dto {fulfillmentCenterId,skuId,itemCount}
     *
     * @return void
     */
    @PostMapping("/inbound")
    public ResponseEntity<Void> createInbound(@RequestBody @Valid InboundRequestDto dto) {

        inboundService.createInbound(dto);
         return ResponseEntity.ok().build();
    }
}
