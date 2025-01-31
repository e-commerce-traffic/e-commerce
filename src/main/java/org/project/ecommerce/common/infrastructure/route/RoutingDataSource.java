package org.project.ecommerce.common.infrastructure.route;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

public class RoutingDataSource extends AbstractRoutingDataSource {

    private static final ThreadLocal<String> CONTEXT = new ThreadLocal<>();

    public static void setDataSource(String dataSourceType) {
        CONTEXT.set(dataSourceType);
    }

    public static void clearDataSource() {
        CONTEXT.remove();
    }

    @Override
    protected Object determineCurrentLookupKey() {
        return CONTEXT.get();
    }
}