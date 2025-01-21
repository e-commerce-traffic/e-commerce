package org.project.ecommerce.common.infrastructure.config;

import org.project.ecommerce.common.infrastructure.route.RoutingDataSource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class DataSourceInterceptor {

    public void setDataSource() {
        if (TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
            RoutingDataSource.setDataSource("replica");
        } else {
            RoutingDataSource.setDataSource("master");
        }
    }
}