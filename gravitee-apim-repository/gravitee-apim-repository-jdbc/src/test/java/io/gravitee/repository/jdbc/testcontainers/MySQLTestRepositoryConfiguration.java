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
package io.gravitee.repository.jdbc.testcontainers;

import io.gravitee.repository.jdbc.AbstractJdbcTestRepositoryConfiguration;
import javax.inject.Inject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.testcontainers.containers.MySQLContainer;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Conditional(MySQLCondition.class)
public class MySQLTestRepositoryConfiguration extends AbstractJdbcTestRepositoryConfiguration {

    @Inject
    private MySQLContainer embeddedMysql;

    @Override
    protected String getJdbcUrl() {
        return getJdbcUrl(embeddedMysql);
    }

    @Bean(destroyMethod = "stop")
    public MySQLContainer embeddedMysql() {
        MySQLContainer container = new MySQLContainer<>();
        container.start();
        return container;
    }
}
