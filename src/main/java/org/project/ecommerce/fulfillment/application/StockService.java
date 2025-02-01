package org.project.ecommerce.fulfillment.application;


import lombok.RequiredArgsConstructor;
import org.project.ecommerce.fulfillment.domain.StockRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class StockService {
    private final StockRepository repository;


    public int getStockByVendorItemId(Long vendorItemId) {
        return repository.findTotalStockByVendorItemId(vendorItemId);
    }


    public int getStock(List<Long> skuIds) {
        return skuIds.stream()
                .map(skuId -> repository.findStockCountBySkuKey(skuId))
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
    }
}
