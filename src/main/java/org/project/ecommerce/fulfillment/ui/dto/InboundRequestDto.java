package org.project.ecommerce.fulfillment.ui.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@NoArgsConstructor
@AllArgsConstructor
public class InboundRequestDto {
    @NotNull
    private Long fulfillmentCenterId;

    @NotNull
    private Long skuId;

    @NotNull
    private int itemCount;


}
