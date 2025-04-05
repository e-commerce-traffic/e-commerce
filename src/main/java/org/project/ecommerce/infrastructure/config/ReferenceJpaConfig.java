package org.project.ecommerce.infrastructure.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
@EnableJpaRepositories(
        basePackages = "org.project.ecommerce.category.domain", // repository 위치
        entityManagerFactoryRef = "referenceEntityManagerFactory",
        transactionManagerRef = "referenceTransactionManager"
)
public class ReferenceJpaConfig {

    @Bean
    public LocalContainerEntityManagerFactoryBean referenceEntityManagerFactory(
            @Qualifier("referenceDataSource") DataSource referenceDataSource,
            EntityManagerFactoryBuilder builder) {

        return builder
                .dataSource(referenceDataSource)
                .packages("org.project.ecommerce.category.domain") // 📌 entity 위치
                .persistenceUnit("reference") // 📌 이름 일관성 추천
                .build();
    }

    @Bean
    public PlatformTransactionManager referenceTransactionManager(
            @Qualifier("referenceEntityManagerFactory") EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }
}
