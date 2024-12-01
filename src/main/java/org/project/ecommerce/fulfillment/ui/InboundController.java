package org.project.ecommerce.fulfillment.ui;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.project.ecommerce.fulfillment.application.InboundService;
import org.project.ecommerce.fulfillment.ui.dto.InboundRequestDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("fulfillment/inbound")
@RequiredArgsConstructor
public class InboundController {
    private final InboundService service;


    @PostMapping
    public ResponseEntity<Void> createInbound(@RequestBody @Valid InboundRequestDto dto) {

         service.createStock(dto);
         return ResponseEntity.ok().build();
    }
}
