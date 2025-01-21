package org.project.ecommerce.fulfillment.application;

import ch.qos.logback.classic.Logger;
import lombok.RequiredArgsConstructor;
import org.project.ecommerce.fulfillment.domain.*;
import org.project.ecommerce.fulfillment.ui.dto.InboundRequestDto;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InboundService {

    private final InboundRepository inboundRepository;
    private final FulfillmentCenterRepository fulfillmentCenterRepository;
    private final SkuRepository skuRepository;
    private final VendorItemSkuRepository vendorItemSkuRepository;

    private final Logger log = (Logger) LoggerFactory.getLogger(getClass());


//    @Transactional(readOnly = true)
//    public int getTotalStockByVendorItem(Long vendorItemId) {
//        // Redis 캐시 키 생성
//        String cacheKey = REDIS_STOCK_KEY_PREFIX + vendorItemId;
//
//        // Redis에서 캐시 데이터 조회
//        Integer cachedStock = (Integer) redisTemplate.opsForValue().get(cacheKey);
//        if (cachedStock != null) {
//            return cachedStock;
//        }
//
//
//        // 2. Redis에 캐시가 없으면 DB 조회
//        List<VendorItemSku> mappings = vendorItemSkuRepository.findByVendorItem_Id(vendorItemId);
////        log.error("Failed to vendorItem , VendorItems: {}", mappings );
//        if (mappings.isEmpty()) {
//            throw new IllegalArgumentException("Invalid Vendor Item ID: " + vendorItemId);
//        }
//
//
//        // SKU의 재고 합산
//        int totalStock = mappings.stream()
//                .mapToInt(mapping -> mapping.getSku().getStockCount())
//                .sum();
//
//        // 3. Redis에 캐시 저장
//        redisTemplate.opsForValue().set(cacheKey, totalStock);
//
//        return totalStock; // 최종 재고 반환
//    }
//
//    @Transactional
//    public void createStock(InboundRequestDto dto) {
//
//        // 1. Fulfillment Center 조회
//        FulfillmentCenter fulfillmentCenter = fulfillmentCenterRepository.findById(dto.getFulfillmentCenterId())
//                .orElseThrow(() -> new IllegalArgumentException("Fulfillment Center not found"));
//
//        // 2. SKU 조회 및 재고 업데이트
//        Sku sku = skuRepository.findById(dto.getSkuId())
//                .orElseThrow(() -> new IllegalArgumentException("SKU not found"));
//        sku.incrementStock(dto.getItemCount());
//        skuRepository.save(sku);
//
//        // 3. Inbound 기록
//        Inbound inbound = Inbound.create(sku, fulfillmentCenter, dto.getItemCount());
//        inboundRepository.save(inbound);
//
//        // 4. Redis 캐시 업데이트 (파이프라이닝 + 초기화 로직 + 에러 핸들링 추가)
//        List<VendorItemSku> vendorItemSkus = sku.getVendorItemSkus();
//
//        try {
//            redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
//                for (VendorItemSku mapping : vendorItemSkus) {
//                    String cacheKey = REDIS_STOCK_KEY_PREFIX + mapping.getVendorItem().getId();
//
//                    // Redis 캐시 데이터 조회
//                    Integer cachedStock = (Integer) redisTemplate.opsForValue().get(cacheKey);
//
//                    // 캐시 업데이트 또는 초기화
//                    if (cachedStock != null) {
//                        connection.set(
//                                redisTemplate.getStringSerializer().serialize(cacheKey),
//                                redisTemplate.getStringSerializer().serialize(String.valueOf(cachedStock + dto.getItemCount()))
//                        );
//                    } else {
//                        // 캐시 데이터가 없는 경우, 초기화
//                        connection.set(
//                                redisTemplate.getStringSerializer().serialize(cacheKey),
//                                redisTemplate.getStringSerializer().serialize(String.valueOf(sku.getStockCount()))
//                        );
//                    }
//                }
//                return null;
//            });
//        } catch (Exception e) {
//            // Redis 캐시 업데이트 실패 시 로깅
//            log.error("Failed to update Redis cache for SKU ID: {}, VendorItems: {}", sku.getId(), vendorItemSkus, e);
//        }
//    }
}



