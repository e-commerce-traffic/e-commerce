package org.project.ecommerce.order.ui.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDto {
    private Long vendorItemId;
    private int itemCount;
}
