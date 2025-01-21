package org.project.ecommerce.order.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.project.ecommerce.fulfillment.domain.Sku;

@Getter
@Entity
@Table(name = "order_item")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    @Column(name = "item_count", nullable = false)
    private int itemCount;

    // 정적 팩토리 메서드를 통한 생성
    public static OrderItem create(VendorItem vendorItem, Sku sku, int itemCount, Order order) {
        return new OrderItem(vendorItem, sku, itemCount, order);
    }

    // Builder 대신 생성자를 통해 불변하게 관계 설정
    private OrderItem(VendorItem vendorItem, Sku sku, int itemCount, Order order) {
        this.vendorItem = vendorItem;
        this.sku = sku;
        this.itemCount = itemCount;
        this.order = order;
    }

}

