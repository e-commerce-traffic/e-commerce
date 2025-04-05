package org.project.ecommerce.config;

import org.project.ecommerce.eventstore.outbox.domain.OutBoxEventRepository;
import org.project.ecommerce.eventstore.outbox.domain.OutboxEventCreator;
import org.project.ecommerce.inventory.application.StockService;
import org.project.ecommerce.category.domain.SkuRepository;
import org.project.ecommerce.inventory.domain.StockRepository;
import org.project.ecommerce.category.domain.VendorItemSkuRepository;
import org.project.ecommerce.order.application.OrderService;
import org.project.ecommerce.order.domain.OrderRepository;
import org.project.ecommerce.order.domain.VendorItemRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.mockito.Mockito.mock;

/**
 * Configuration class for tests.
 * Provides mock or in-memory implementations of dependencies for unit testing.
 */
@Configuration
public class TestConfig {

    @Bean
    public OrderRepository orderRepository() {
        return new InMemoryOrderRepository();
    }
    
    @Bean
    public OutBoxEventRepository outBoxEventRepository() {
        return mock(OutBoxEventRepository.class);
    }
    
    @Bean
    public SkuRepository skuRepository() {
        return mock(SkuRepository.class);
    }
    
    @Bean
    public StockService stockService() {
        return mock(StockService.class);
    }
    
    @Bean
    public VendorItemRepository vendorItemRepository() {
        return mock(VendorItemRepository.class);
    }
    
    @Bean
    public VendorItemSkuRepository vendorItemSkuRepository() {
        return mock(VendorItemSkuRepository.class);
    }
    
    @Bean
    public StockRepository stockRepository() {
        return mock(StockRepository.class);
    }
    
    @Bean
    public OutboxEventCreator outboxEventCreator() {
        return mock(OutboxEventCreator.class);
    }
    
    @Bean
    public ApplicationEventPublisher applicationEventPublisher() {
        return mock(ApplicationEventPublisher.class);
    }
    
    @Bean
    public OrderService orderService(
            VendorItemRepository vendorItemRepository,
            SkuRepository skuRepository,
            OrderRepository orderRepository,
            VendorItemSkuRepository vendorItemSkuRepository,
            OutBoxEventRepository outBoxEventRepository,
            OutboxEventCreator outboxEventCreator,
            StockService stockService,
            ApplicationEventPublisher applicationEventPublisher) {
        
        return new OrderService(
                vendorItemRepository,
                skuRepository,
                null, // InboundRepository not needed for tests
                orderRepository,
                vendorItemSkuRepository,
                outBoxEventRepository,
                outboxEventCreator,
                stockService,
                applicationEventPublisher
        );
    }
}