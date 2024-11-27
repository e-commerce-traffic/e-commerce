package org.project.ecommerce.fulfillment.domain;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class FulfillmentCenter {
    private final Long id;
    private final String name;
    private final List<Stock> stocks = new ArrayList<>();

    public FulfillmentCenter(Long id, String name) {
        this.id = id;
        this.name = name;
    }

}
