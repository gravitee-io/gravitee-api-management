/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.groupgti.shared.gravitee.repository.jdbc.configuration;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.util.Map;
import java.util.Properties;
import javax.sql.DataSource;
import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;

/**
 *
 * @author njt
 */
public class AbstractJdbcRepositoryConfiguration implements ApplicationContextAware {
    
    @SuppressWarnings("constantname")
    private static final Logger logger = LoggerFactory.getLogger(AbstractJdbcRepositoryConfiguration.class);
    
    @Value("${management.jdbc.url}")
    private String jdbcUrl;
    
    @Value("${management.jdbc.username}")
    private String username;
    
    @Value("${management.jdbc.password}")
    private String password;

    public AbstractPlatformTransactionManager graviteeTransactionManager(DataSource dataSource) {
        logger.debug("AbstractJdbcRepositoryConfiguration.graviteeTransactionManager()");
        return new DataSourceTransactionManager(dataSource);
    }
    
    private ConfigurableApplicationContext getRoot(ApplicationContext appContext) {
        while (appContext.getParent() != null) {
            appContext = appContext.getParent();
        }
        return (ConfigurableApplicationContext) appContext;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        logger.debug("AbstractJdbcRepositoryConfiguration.setApplicationContext({})", applicationContext);
        logger.debug("jdbcURL: {}", jdbcUrl);
        
        ConfigurableApplicationContext rootAppContext = getRoot(applicationContext);
        Map<String, DataSource> existing = rootAppContext.getBeansOfType(DataSource.class);
        if (!existing.isEmpty()) {
            logger.debug("Existing DataSource: {}", existing);
        } else {
            ConfigurableListableBeanFactory beanFactory = rootAppContext.getBeanFactory();
            DataSource dataSource = dataSource();
            beanFactory.registerSingleton("DataSource", dataSource);
            beanFactory.registerSingleton("graviteeTransactionManager", graviteeTransactionManager(dataSource));
        }
        
    }
    
    public DataSource dataSource() {
        logger.debug("AbstractJdbcRepositoryConfiguration.dataSource()");
        Properties props = new Properties();
        props.setProperty("jdbcUrl", jdbcUrl);
        if (username != null) {
            props.setProperty("username", username);
        }
        if (password != null) {
            props.setProperty("password", password);
        }
        props.setProperty("minimumIdle", "1");
        logger.debug("Setting datasource using: {}", props);
        HikariConfig hikariConfig = new HikariConfig(props);
        DataSource dataSource = new HikariDataSource(hikariConfig);
        runLiquibase(dataSource);
        return dataSource;
    }    
    
    private Liquibase createLiquibase(Connection conn) throws LiquibaseException {
        Liquibase liquibase = new Liquibase("liquibase/master.yml"
                , new ClassLoaderResourceAccessor(this.getClass().getClassLoader()), new JdbcConnection(conn));
        liquibase.setIgnoreClasspathPrefix(true);

        return liquibase;
    }

    private void performUpdate(Liquibase liquibase) throws LiquibaseException {
        liquibase.update((Contexts) null);
    }    

    private void runLiquibase(DataSource dataSource) {
        logger.debug("Running LiquiBase on {}", dataSource);
        try (Connection conn = dataSource.getConnection()) {
            Liquibase liquibase = createLiquibase(conn);
            performUpdate(liquibase);
        } catch (Exception ex) {
            logger.error("Failed to set up database: ", ex);
        }
    }
    
}
