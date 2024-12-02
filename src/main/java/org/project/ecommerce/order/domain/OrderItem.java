package org.project.ecommerce.order.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.project.ecommerce.fulfillment.domain.Sku;

@Getter
@Entity
@Table(name = "order_item")
@NoArgsConstructor
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_item_id")
    private Long id;


    @ManyToOne
    @JoinColumn(name = "sku_id", nullable = false)
    private Sku sku;

    @ManyToOne(optional = false)
    @JoinColumn(name = "vendor_item_id", nullable = false)
    private VendorItem vendorItem;

    @ManyToOne(optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false)
    private int itemCount;

    public static OrderItem create(VendorItem vendorItem,Sku sku ,int itemCount,Order order) {

        return OrderItem.builder()
                .vendorItem(vendorItem)
                .sku(sku)
                .itemCount(itemCount)
                .order(order)
                .build();
    }
    @Builder
    public OrderItem(VendorItem vendorItem, Sku sku, int itemCount, Order order) {
        this.vendorItem = vendorItem;
        this.sku = sku;
        this.itemCount = itemCount;
        this.order = order;
    }

    // 양방향 관계를 설정하기 위해 Order 설정
    public void setOrder(Order order) {
        this.order = order;
    }


}
