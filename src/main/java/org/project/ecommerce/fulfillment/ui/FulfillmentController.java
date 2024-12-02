package org.project.ecommerce.fulfillment.ui;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.project.ecommerce.fulfillment.application.InboundService;
import org.project.ecommerce.fulfillment.ui.dto.InboundRequestDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/fulfillment")
@RequiredArgsConstructor
public class FulfillmentController {
    private final InboundService service;


    @GetMapping("/stock/{vendorItemId}")
    public ResponseEntity<Integer> getStock(@PathVariable Long vendorItemId){
        int stock = service.getStockByVendorItem(vendorItemId);
        return ResponseEntity.ok(stock);
    }



    @PostMapping("/inbound")
    public ResponseEntity<Void> createInbound(@RequestBody @Valid InboundRequestDto dto) {

         service.createStock(dto);
         return ResponseEntity.ok().build();
    }
}
