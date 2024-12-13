package org.project.ecommerce.order.application;

import lombok.RequiredArgsConstructor;
import org.project.ecommerce.fulfillment.domain.*;
import org.project.ecommerce.order.domain.*;
import org.project.ecommerce.order.ui.dto.OrderRequestDto;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final RedisTemplate<String,String> redisTemplate;
    private final VendorItemRepository vendorItemRepository;
    private final SkuRepository skuRepository;
    private final InboundRepository inboundRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public void createOrder(OrderRequestDto dto, String idempotencyKey) {


//        orderRepository.save(order);
    }

}
