package com.demo.tourwave.bootstrap

import jakarta.persistence.EntityManagerFactory
import org.flywaydb.core.Flyway
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Environment
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.orm.jpa.JpaVendorAdapter
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import javax.sql.DataSource

@Configuration
@Profile("mysql", "mysql-test")
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = ["com.demo.tourwave.adapter.out.persistence.jpa"])
@EntityScan(basePackages = ["com.demo.tourwave.adapter.out.persistence.jpa"])
class MysqlPersistenceConfig {
    @Bean
    @ConfigurationProperties("spring.datasource")
    fun dataSourceProperties(): DataSourceProperties = DataSourceProperties()

    @Bean
    fun dataSource(dataSourceProperties: DataSourceProperties): DataSource =
        dataSourceProperties.initializeDataSourceBuilder().build()

    @Bean(initMethod = "migrate")
    fun flyway(dataSource: DataSource): Flyway {
        return Flyway.configure()
            .baselineOnMigrate(true)
            .locations("classpath:db/migration")
            .dataSource(dataSource)
            .load()
    }

    @Bean
    fun jpaVendorAdapter(): JpaVendorAdapter = HibernateJpaVendorAdapter()

    @Bean
    fun entityManagerFactory(
        dataSource: DataSource,
        jpaVendorAdapter: JpaVendorAdapter,
        environment: Environment
    ): LocalContainerEntityManagerFactoryBean {
        return LocalContainerEntityManagerFactoryBean().apply {
            setDataSource(dataSource)
            setJpaVendorAdapter(jpaVendorAdapter)
            setPackagesToScan("com.demo.tourwave.adapter.out.persistence.jpa")
            setJpaPropertyMap(
                mapOf(
                    "hibernate.hbm2ddl.auto" to environment.getProperty(
                        "tourwave.persistence.hbm2ddl-auto",
                        "validate"
                    ),
                    "hibernate.dialect" to environment.getProperty(
                        "tourwave.persistence.hibernate-dialect",
                        "org.hibernate.dialect.MySQLDialect"
                    ),
                    "hibernate.jdbc.time_zone" to "UTC",
                    "hibernate.format_sql" to environment.getProperty("spring.jpa.properties.hibernate.format_sql", "false")
                )
            )
        }
    }

    @Bean
    fun transactionManager(entityManagerFactory: EntityManagerFactory): PlatformTransactionManager {
        return JpaTransactionManager(entityManagerFactory)
    }
}
