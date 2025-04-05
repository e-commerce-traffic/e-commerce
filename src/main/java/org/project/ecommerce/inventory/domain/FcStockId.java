package org.project.ecommerce.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FcStockId implements Serializable {
    @Column(name = "fulfillment_center_id", nullable = false)
    private Long fulfillmentCenterId;

    @Column(name = "sku_id", nullable = false)
    private Long skuId;


    private FcStockId(Long fulfillmentCenterId, Long skuId) {
        this.fulfillmentCenterId = fulfillmentCenterId;
        this.skuId = skuId;
    }

    public static FcStockId of(Long fulfillmentCenterId, Long skuId) {
        if (fulfillmentCenterId == null || skuId == null) {
            throw new IllegalArgumentException("센터 ID와 SKU ID는 null일 수 없습니다.");
        }
        return new FcStockId(fulfillmentCenterId, skuId);
    }
}
