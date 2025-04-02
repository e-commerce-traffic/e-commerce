package org.project.ecommerce.common.infrastructure.config;

import jakarta.persistence.EntityManagerFactory;
import org.project.ecommerce.common.infrastructure.route.RoutingDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * 대용량 트래픽(최대 500M TPS)을 위한 트랜잭션 설정
 * - 샤딩 구성에 필요한 트랜잭션 관리
 * - 트랜잭션 완료 후 샤드 라우팅 정보 초기화
 */
@Configuration
@EnableTransactionManagement
public class TransactionConfig {

    /**
     * 엔티티 매니저 팩토리 구성
     * - 샤딩을 위한 라우팅 데이터소스 사용
     * - 성능 최적화를 위한 JPA 설정 추가
     */
    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            @Qualifier("dataSource") DataSource dataSource,
            JpaProperties jpaProperties) {

        LocalContainerEntityManagerFactoryBean factoryBean = new LocalContainerEntityManagerFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setPackagesToScan("org.project.ecommerce"); // 엔티티 패키지
        factoryBean.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        
        // 기존 JPA 속성 가져오기
        Properties properties = new Properties();
        properties.putAll(jpaProperties.getProperties());
        
        // 성능 최적화 설정 추가
        properties.put("hibernate.jdbc.batch_size", "50");
        properties.put("hibernate.order_inserts", "true");
        properties.put("hibernate.order_updates", "true");
        properties.put("hibernate.jdbc.batch_versioned_data", "true");
        
        factoryBean.setJpaProperties(properties);
        return factoryBean;
    }

    /**
     * 트랜잭션 매니저 구성
     * - 트랜잭션 완료 시 샤드 라우팅 정보 자동 초기화
     */
    @Bean
    public JpaTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        JpaTransactionManager transactionManager = new JpaTransactionManager(entityManagerFactory) {
            @Override
            protected void doCleanupAfterCompletion(Object transaction) {
                // 트랜잭션 완료 후 샤드 키 초기화
                RoutingDataSource.clearDataSource();
                super.doCleanupAfterCompletion(transaction);
            }
        };
        return transactionManager;
    }
}
