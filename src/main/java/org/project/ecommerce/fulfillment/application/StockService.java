package org.project.ecommerce.fulfillment.application;


import lombok.RequiredArgsConstructor;
import org.project.ecommerce.fulfillment.domain.StockRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StockService {
    private final StockRepository repository;

    public int getStock(List<Long> skuIds) {
        return skuIds.stream()
                .map(skuId -> repository.findStockBySkuKey(skuId))
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
    }
}
