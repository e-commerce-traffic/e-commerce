package org.project.ecommerce.order.ui;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.project.ecommerce.order.application.OrderService;
import org.project.ecommerce.order.ui.dto.OrderRequestDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<Void> createOrder(@RequestHeader("Idempotency-Key") String idempotencyKey, @RequestBody @Valid OrderRequestDto dto){
        orderService.createOrder(dto, idempotencyKey);
        return ResponseEntity.ok().build();

    }
}
