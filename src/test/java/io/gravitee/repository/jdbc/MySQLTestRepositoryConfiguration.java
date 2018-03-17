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

import com.wix.mysql.EmbeddedMysql;
import com.wix.mysql.config.MysqldConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;

import javax.inject.Inject;

import static com.wix.mysql.EmbeddedMysql.anEmbeddedMysql;
import static com.wix.mysql.distribution.Version.v5_7_latest;
import static io.gravitee.repository.jdbc.common.AbstractJdbcRepositoryConfiguration.setEscapeReservedWordsChar;
import static java.lang.String.format;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Conditional(MySQLCondition.class)
public class MySQLTestRepositoryConfiguration extends AbstractJdbcTestRepositoryConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(MySQLTestRepositoryConfiguration.class);

    @Inject
    private EmbeddedMysql embeddedMysql;

    @Override
    String getJdbcUrl() {
        final MysqldConfig config = embeddedMysql.getConfig();
        return format("jdbc:mysql://localhost:%s/gravitee?useSSL=false&user=%s&password=%s", config.getPort(),
                config.getUsername(), config.getPassword());
    }

    @Bean(destroyMethod = "stop")
    public EmbeddedMysql embeddedMysql() {
        setEscapeReservedWordsChar('`');
        return anEmbeddedMysql(v5_7_latest).addSchema("gravitee").start();
    }
}