package org.project.ecommerce.infrastructure.config;

import org.project.ecommerce.infrastructure.route.RoutingDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * 대용량 트래픽(최대 5M TPS)을 위한 데이터소스 설정
 * - 샤딩 구성을 위한 다중 데이터소스 설정
 * - 실제 환경에서는 64개 샤드를 설정해야 하지만 여기서는 2개만 예시로 설정
 */
@Configuration
public class DataSourceConfig {

    /**
     * Shard 1 데이터소스
     */
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.shard1")
    public DataSource shard1DataSource() {
        return DataSourceBuilder.create().build();
    }

    /**
     * Shard 2 데이터소스
     */
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.shard2")
    public DataSource shard2DataSource() {
        return DataSourceBuilder.create().build();
    }

    /**
     * reference 데이터소스
     */
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.reference")
    public DataSource referenceDataSource() {
        return DataSourceBuilder.create().build();
    }

    /**
     * 동적 라우팅 데이터소스 구성
     * - 64개 샤드를 지원하도록 설계되어 있지만 예시로 2개만 설정
     * - 실제 환경에서는 모든 샤드 데이터소스를 설정해야 함
     */
    @Bean
    public DataSource routingDataSource(
            @Qualifier("shard1DataSource") DataSource shard1DataSource,
            @Qualifier("shard2DataSource") DataSource shard2DataSource) {
        
        RoutingDataSource routingDataSource = new RoutingDataSource();

        Map<Object, Object> targetDataSources = new HashMap<>();
        // 기본 샤드 설정 (실제로는 64개 모두 추가해야 함)
        targetDataSources.put("db-shard1", shard1DataSource);
        targetDataSources.put("db-shard2", shard2DataSource);
        
        // 샤드 3-64는 실제 구현 시 추가 필요
        // targetDataSources.put("db-shard3", shard3DataSource);
        // ...
        // targetDataSources.put("db-shard64", shard64DataSource);

        routingDataSource.setTargetDataSources(targetDataSources);
        routingDataSource.setDefaultTargetDataSource(shard1DataSource);
        return routingDataSource;
    }
    
    /**
     * 최종 데이터소스 생성 (성능 최적화를 위한 Lazy 연결 적용)
     * - 실제 쿼리 실행 시까지 연결을 지연시켜 리소스 최적화
     */
    @Primary
    @Bean
    public DataSource dataSource(@Qualifier("routingDataSource") DataSource routingDataSource) {
        return new LazyConnectionDataSourceProxy(routingDataSource);
    }
}
