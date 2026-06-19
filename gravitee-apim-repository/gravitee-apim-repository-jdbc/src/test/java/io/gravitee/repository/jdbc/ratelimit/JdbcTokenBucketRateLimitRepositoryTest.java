/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.repository.jdbc.ratelimit;

import io.gravitee.repository.jdbc.JdbcTestRepositoryConfiguration;
import io.gravitee.repository.ratelimit.AbstractTokenBucketRateLimitRepositoryContractTest;
import io.gravitee.repository.ratelimit.api.TokenBucketRateLimitRepository;
import io.gravitee.repository.ratelimit.model.TokenBucket;
import java.sql.Connection;
import java.util.Properties;
import javax.sql.DataSource;
import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Runs the shared {@link AbstractTokenBucketRateLimitRepositoryContractTest} against a JDBC database.
 * Boots the standard JDBC test context (a database container + the component-scanned repository) and
 * applies the Liquibase schema once, with the same prefixes the repository reads so the table names
 * line up. The database is selected by the {@code jdbcType} property (PostgreSQL by default). No
 * per-test cleanup is needed: the container is fresh and the contract suite uses a distinct key per test.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = JdbcTestRepositoryConfiguration.class)
class JdbcTokenBucketRateLimitRepositoryTest extends AbstractTokenBucketRateLimitRepositoryContractTest {

    private static volatile boolean schemaReady = false;

    @Autowired
    private TokenBucketRateLimitRepository<TokenBucket> tokenBucketRepository;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Properties graviteeProperties;

    @Override
    protected TokenBucketRateLimitRepository<TokenBucket> createRepository() {
        ensureSchema();
        return tokenBucketRepository;
    }

    private synchronized void ensureSchema() {
        if (schemaReady) {
            return;
        }
        String managementPrefix = graviteeProperties.getProperty("management.jdbc.prefix", "");
        String rateLimitPrefix = graviteeProperties.getProperty("ratelimit.jdbc.prefix", "");
        System.setProperty("liquibase.databaseChangeLogTableName", managementPrefix + "databasechangelog");
        System.setProperty("liquibase.databaseChangeLogLockTableName", managementPrefix + "databasechangeloglock");
        System.setProperty("gravitee_prefix", managementPrefix);
        System.setProperty("gravitee_rate_limit_prefix", rateLimitPrefix);

        try (Connection connection = dataSource.getConnection()) {
            new Liquibase(
                "liquibase/master.yml",
                new ClassLoaderResourceAccessor(getClass().getClassLoader()),
                new JdbcConnection(connection)
            ).update((Contexts) null);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to set up the JDBC schema via Liquibase", e);
        }
        schemaReady = true;
    }
}
