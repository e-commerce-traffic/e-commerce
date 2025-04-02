package org.project.ecommerce.common.infrastructure.route;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * 대용량 트래픽(최대 500M TPS)을 위한 동적 데이터소스 라우팅
 * - ThreadLocal 기반으로 현재 트랜잭션의 데이터소스 키를 관리
 */
public class RoutingDataSource extends AbstractRoutingDataSource {

    private static final Logger log = LoggerFactory.getLogger(RoutingDataSource.class);
    private static final ThreadLocal<String> CONTEXT = new ThreadLocal<>();

    /**
     * 샤딩 키를 통해 특정 샤드를 선택하도록 설정
     * @param shardKey 데이터소스 식별자 (예: db-shard1, db-shard2, ...)
     */
    public static void setDataSource(String shardKey) {
        if (shardKey == null) {
            log.warn("Attempted to set null shard key, using default datasource");
            return;
        }
        
        String previousKey = CONTEXT.get();
        if (previousKey != null && !previousKey.equals(shardKey)) {
            log.debug("Changing shard key from {} to {}", previousKey, shardKey);
        }
        
        CONTEXT.set(shardKey);
    }

    /**
     * 현재 설정된 데이터소스 키 반환
     * @return 현재 사용 중인 데이터소스 키
     */
    public static String getCurrentDataSource() {
        return CONTEXT.get();
    }

    /**
     * 데이터소스 설정 제거 (트랜잭션 종료 후 호출 필요)
     */
    public static void clearDataSource() {
        CONTEXT.remove();
    }

    /**
     * 현재 스레드의 데이터소스 키 확인
     * @return 데이터소스 키
     */
    @Override
    protected Object determineCurrentLookupKey() {
        String key = CONTEXT.get();
        if (key == null) {
            log.trace("No specific shard key set, using default datasource");
        } else {
            log.trace("Using shard key: {}", key);
        }
        return key;
    }
}