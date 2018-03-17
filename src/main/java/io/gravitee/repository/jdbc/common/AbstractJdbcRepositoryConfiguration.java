/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.repository.jdbc.common;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Map;

/**
 *
 * @author njt
 */
public abstract class AbstractJdbcRepositoryConfiguration implements ApplicationContextAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractJdbcRepositoryConfiguration.class);

    private static final boolean DEFAULT_AUTO_COMMIT = true;
    private static final long DEFAULT_CONNECTION_TIMEOUT = 10000;
    private static final long DEFAULT_IDLE_TIMEOUT = 600000;
    private static final long DEFAULT_MAX_LIFETIME = 1800000;
    private static final int DEFAULT_MIN_IDLE = 10;
    private static final int DEFAULT_MAX_POOL_SIZE = 10;
    private static final boolean DEFAULT_REGISTER_MBEANS = true;

    @Autowired
    private Environment env;

    private static char escapeReservedWordsChar = '\"';

    public static void setEscapeReservedWordsChar(char escapeReservedWordsChar) {
        AbstractJdbcRepositoryConfiguration.escapeReservedWordsChar = escapeReservedWordsChar;
    }

    public static String escapeReservedWord(final String word) {
        return escapeReservedWordsChar + word + escapeReservedWordsChar;
    }

    @Override
    public void setApplicationContext(final ApplicationContext applicationContext) {
        LOGGER.debug("AbstractJdbcRepositoryConfiguration.setApplicationContext({})", applicationContext);
        final ConfigurableApplicationContext appContext;
        final ApplicationContext applicationContextParent = applicationContext.getParent();
        if (applicationContextParent == null) {
            appContext = (ConfigurableApplicationContext) applicationContext;
        } else {
            appContext = (ConfigurableApplicationContext) applicationContextParent;
        }
        final Map<String, DataSource> dataSources = appContext.getBeansOfType(DataSource.class);
        if (dataSources.isEmpty()) {
            final ConfigurableListableBeanFactory beanFactory = appContext.getBeanFactory();
            final DataSource dataSource = graviteeDataSource();
            beanFactory.registerSingleton("graviteeDataSource", dataSource);
            beanFactory.registerSingleton("graviteeTransactionManager", new DataSourceTransactionManager(dataSource));
        }
    }

    private synchronized DataSource graviteeDataSource() {
        final HikariConfig dsConfig = new HikariConfig();
        dsConfig.setPoolName("gravitee-jdbc-pool-1");

        final String jdbcUrl = readPropertyValue("management.jdbc.url");
        if (jdbcUrl != null && "mysql".equals(jdbcUrl.split(":")[1])) {
            escapeReservedWordsChar = '`';
        }

        dsConfig.setJdbcUrl(jdbcUrl);
        dsConfig.setUsername(readPropertyValue("management.jdbc.username"));
        dsConfig.setPassword(readPropertyValue("management.jdbc.password", false));
        // Pooling
        dsConfig.setAutoCommit(readPropertyValue("management.jdbc.pool.autoCommit", Boolean.class, DEFAULT_AUTO_COMMIT));
        dsConfig.setConnectionTimeout(readPropertyValue("management.jdbc.pool.connectionTimeout", Long.class, DEFAULT_CONNECTION_TIMEOUT));
        dsConfig.setIdleTimeout(readPropertyValue("management.jdbc.pool.idleTimeout", Long.class, DEFAULT_IDLE_TIMEOUT));
        dsConfig.setMaxLifetime(readPropertyValue("management.jdbc.pool.maxLifetime", Long.class, DEFAULT_MAX_LIFETIME));
        dsConfig.setMinimumIdle(readPropertyValue("management.jdbc.pool.minIdle", Integer.class, DEFAULT_MIN_IDLE));
        dsConfig.setMaximumPoolSize(readPropertyValue("management.jdbc.pool.maxPoolSize", Integer.class, DEFAULT_MAX_POOL_SIZE));
        dsConfig.setRegisterMbeans(readPropertyValue("management.jdbc.pool.registerMbeans", Boolean.class, DEFAULT_REGISTER_MBEANS));

        final DataSource dataSource = new HikariDataSource(dsConfig);
        runLiquibase(dataSource);
        return dataSource;
    }

    @Bean
    public JdbcTemplate graviteeJdbcTemplate(final DataSource dataSource) {
        LOGGER.debug("AbstractJdbcRepositoryConfiguration.graviteeJdbcTemplate()");
        return new JdbcTemplate(dataSource);
    }

    private void runLiquibase(DataSource dataSource) {
        LOGGER.debug("Running Liquibase on {}", dataSource);

        System.setProperty("liquibase.databaseChangeLogTableName", "databasechangelog");
        System.setProperty("liquibase.databaseChangeLogLockTableName", "databasechangeloglock");

        try (Connection conn = dataSource.getConnection()) {
            final Liquibase liquibase = new Liquibase("liquibase/master.yml"
                    , new ClassLoaderResourceAccessor(this.getClass().getClassLoader()), new JdbcConnection(conn));
            liquibase.setIgnoreClasspathPrefix(true);
            liquibase.update((Contexts) null);
        } catch (Exception ex) {
            LOGGER.error("Failed to set up database: ", ex);
        }
    }

    private String readPropertyValue(String propertyName) {
        return readPropertyValue(propertyName, true);
    }

    private String readPropertyValue(String propertyName, final boolean displayOnLog) {
        return readPropertyValue(propertyName, String.class, null, displayOnLog);
    }

    private <T> T readPropertyValue(String propertyName, Class<T> propertyType, T defaultValue) {
        return readPropertyValue(propertyName, propertyType, defaultValue, true);
    }

    private <T> T readPropertyValue(String propertyName, Class<T> propertyType, T defaultValue, final boolean displayOnLog) {
        final T value = env.getProperty(propertyName, propertyType, defaultValue);
        LOGGER.debug("Reading property {}: {}", propertyName, displayOnLog ? value : "********");
        return value;
    }
}