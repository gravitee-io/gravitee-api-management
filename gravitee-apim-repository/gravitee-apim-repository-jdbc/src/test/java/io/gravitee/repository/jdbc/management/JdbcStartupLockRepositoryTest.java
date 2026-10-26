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
package io.gravitee.repository.jdbc.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.repository.exceptions.StartupLockTimeoutException;
import io.gravitee.repository.management.AbstractManagementRepositoryTest;
import io.gravitee.repository.management.api.StartupLock;
import io.gravitee.repository.management.api.StartupLockRepository;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Properties;
import org.junit.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.JdbcDatabaseContainer;

public class JdbcStartupLockRepositoryTest extends AbstractManagementRepositoryTest {

    private static final Duration SHORT_WAIT = Duration.ofSeconds(1);

    @Inject
    private StartupLockRepository startupLockRepository;

    @Inject
    private JdbcDatabaseContainer<?> jdbcDatabaseContainer;

    @Inject
    private Properties graviteeProperties;

    @Override
    protected String getTestCasesPath() {
        return null;
    }

    @Test
    public void should_free_the_lock_when_the_holder_connection_dies() throws Exception {
        RecordingDataSource holderDataSource = new RecordingDataSource(jdbcDatabaseContainer);
        StartupLock holder = new JdbcStartupLockRepository(
            holderDataSource,
            graviteeProperties.getProperty("management.jdbc.prefix", "")
        ).acquire(SHORT_WAIT);
        try {
            assertThatThrownBy(() -> startupLockRepository.acquire(SHORT_WAIT)).isInstanceOf(StartupLockTimeoutException.class);

            holderDataSource.lastConnection.abort(Runnable::run);

            try (StartupLock next = startupLockRepository.acquire(Duration.ofSeconds(30))) {
                assertThat(next).isNotNull();
            }
        } finally {
            holder.close();
        }
    }

    private static class RecordingDataSource extends DriverManagerDataSource {

        private Connection lastConnection;

        RecordingDataSource(JdbcDatabaseContainer<?> container) {
            super(container.getJdbcUrl(), container.getUsername(), container.getPassword());
        }

        @Override
        public Connection getConnection() throws SQLException {
            lastConnection = super.getConnection();
            return lastConnection;
        }
    }
}
