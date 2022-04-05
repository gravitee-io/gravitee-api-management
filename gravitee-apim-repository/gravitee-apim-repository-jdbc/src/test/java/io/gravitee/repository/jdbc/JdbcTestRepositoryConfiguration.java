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
package io.gravitee.repository.jdbc;

import static io.gravitee.repository.jdbc.common.AbstractJdbcRepositoryConfiguration.setEscapeReservedWordFromDatabaseType;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.testcontainers.containers.*;
import org.testcontainers.containers.startupcheck.MinimumDurationRunningStartupCheckStrategy;
import org.testcontainers.utility.DockerImageName;

/**
 *
 * @author njt
 */
@ComponentScan("io.gravitee.repository.jdbc")
public class JdbcTestRepositoryConfiguration {

    @Value("${jdbcType:postgresql}")
    private String jdbcType;

    @Bean
    public DataSource graviteeDataSource(JdbcDatabaseContainer container) {
        final HikariConfig dsConfig = getHikariConfig(container);
        return new HikariDataSource(dsConfig);
    }

    @Bean(destroyMethod = "stop")
    public JdbcDatabaseContainer container() {
        String databaseType;
        String tag;
        DatabaseConfigurationEnum tcDatabase;

        int indexOfTag = jdbcType.indexOf('~');
        if (indexOfTag > 0) {
            databaseType = jdbcType.substring(0, indexOfTag);
            tcDatabase = DatabaseConfigurationEnum.valueOf(databaseType.toUpperCase());
            tag = jdbcType.substring(indexOfTag + 1);
        } else {
            databaseType = jdbcType;
            tcDatabase = DatabaseConfigurationEnum.valueOf(databaseType.toUpperCase());
            tag = tcDatabase.getDefaultTag();
        }
        setEscapeReservedWordFromDatabaseType(databaseType);

        JdbcDatabaseContainer containerBean;
        DockerImageName dockerImageName = DockerImageName.parse(tcDatabase.getDockerImageName()).withTag(tag);
        switch (tcDatabase) {
            case MYSQL:
                // It appears that the limitation is not entirely due to the DB engine configuration, but also in the way I/O are managed between container and host.
                // I let this configuration for documentation purpose, but it does not solve our performance issue.
                // See --> https://dev.mysql.com/doc/refman/8.0/en/innodb-parameters.html
                containerBean = new MySQLContainer<>(dockerImageName).withCommand("mysqld --innodb-buffer-pool-size=1G");
                break;
            case MARIADB:
                // It appears that the limitation is not entirely due to the DB engine configuration, but also in the way I/O are managed between container and host.
                // I let this configuration for documentation purpose, but it does not solve our performance issue.
                // See --> https://mariadb.com/kb/en/innodb-system-variables/
                containerBean = new MariaDBContainer<>(dockerImageName).withCommand("mysqld --innodb-buffer-pool-size=1G");
                break;
            case SQLSERVER:
                containerBean = new MSSQLServerContainer<>(dockerImageName);
                break;
            case POSTGRESQL:
            default:
                containerBean = new PostgreSQLContainer<>(dockerImageName);
                break;
        }
        containerBean
            .withUrlParam("TC_DAEMON", "true") // https://www.testcontainers.org/modules/databases/jdbc/#running-container-in-daemon-mode
            .withTmpFs(Collections.singletonMap("/testtmpfs", "rw")); // https://www.testcontainers.org/modules/databases/jdbc/#running-container-with-tmpfs-options
        containerBean.start();
        return containerBean;
    }

    private HikariConfig getHikariConfig(JdbcDatabaseContainer container) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(container.getJdbcUrl());
        hikariConfig.setUsername(container.getUsername());
        hikariConfig.setPassword(container.getPassword());
        hikariConfig.setDriverClassName(container.getDriverClassName());
        Properties properties = new Properties();
        properties.setProperty("useSSL", "false");
        hikariConfig.setDataSourceProperties(properties);
        return hikariConfig;
    }

    @Bean
    public DataSourceTransactionManager graviteeTransactionManager(@Autowired DataSource ds) {
        return new DataSourceTransactionManager(ds);
    }
}
