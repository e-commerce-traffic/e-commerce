package org.project.ecommerce.fulfillment.ui;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.project.ecommerce.fulfillment.application.InboundService;
import org.project.ecommerce.fulfillment.application.StockService;
import org.project.ecommerce.fulfillment.ui.dto.InboundRequestDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/stock")
@RequiredArgsConstructor
public class FulfillmentController {
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
