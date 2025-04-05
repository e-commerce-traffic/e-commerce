package org.project.ecommerce.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.project.ecommerce.infrastructure.route.RoutingDataSource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 데이터소스 인터셉터 - 샤딩 환경에 맞게 수정
 * - 샤드 라우팅이 이미 설정된 경우 충돌하지 않도록 개선
 */
@Component
@Slf4j
public class DataSourceInterceptor {

    /**
     * 데이터소스 설정 (샤딩과 호환되도록 수정)
     * - 샤드 라우팅이 우선 적용되고, 샤드가 설정되지 않은 경우에만 읽기/쓰기 분리 적용
     */
    public void setDataSource() {
        // 이미 샤드가 설정되어 있는 경우 무시 (샤딩 우선)
        if (RoutingDataSource.getCurrentDataSource() != null) {
            log.debug("Shard already set to {}, skipping master/replica routing", 
                    RoutingDataSource.getCurrentDataSource());
            return;
        }
        
        // 샤드가 설정되지 않은 경우에만 읽기/쓰기 분리 적용
        if (TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
            log.debug("Setting read-only datasource (replica)");
            RoutingDataSource.setDataSource("db-shard2"); // 읽기 전용은 shard2로 라우팅 
        } else {
            log.debug("Setting write datasource (master)");
            RoutingDataSource.setDataSource("db-shard1"); // 쓰기는 shard1으로 라우팅
        }
    }
}