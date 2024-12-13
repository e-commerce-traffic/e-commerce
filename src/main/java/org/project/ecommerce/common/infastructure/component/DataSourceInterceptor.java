package org.project.ecommerce.common.infastructure.component;

import org.project.ecommerce.common.infastructure.route.RoutingDataSource;
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