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

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import javax.sql.DataSource;

import static io.gravitee.repository.jdbc.common.AbstractJdbcRepositoryConfiguration.setEscapeReservedWordFromJDBCUrl;

/**
 *
 * @author njt
 */
@ComponentScan("io.gravitee.repository.jdbc")
public abstract class AbstractJdbcTestRepositoryConfiguration {

    abstract String getJdbcUrl();

    @Bean
    public DataSource graviteeDataSource() {
        final HikariConfig dsConfig = new HikariConfig();
        final String jdbcUrl = getJdbcUrl();
        dsConfig.setJdbcUrl(jdbcUrl);
        setEscapeReservedWordFromJDBCUrl(jdbcUrl);
        return new HikariDataSource(dsConfig);
    }
}