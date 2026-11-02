package com.quickcart.warehouse.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

/** Spring configuration: DataSource, JdbcTemplate and the transaction manager. */
@Configuration
@EnableTransactionManagement
public class DatabaseConfig {

    @Bean
    public DataSource dataSource(@Value("${warehouse.datasource.url}") String url,
                                 @Value("${warehouse.datasource.username}") String username,
                                 @Value("${warehouse.datasource.password}") String password) {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(url);
        cfg.setUsername(username);
        cfg.setPassword(password);
        cfg.setDriverClassName("com.mysql.cj.jdbc.Driver");
        cfg.setMaximumPoolSize(5);
        cfg.setPoolName("warehouse-pool");
        return new HikariDataSource(cfg);
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    /** Required so @Transactional gives us atomic order placement. */
    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
