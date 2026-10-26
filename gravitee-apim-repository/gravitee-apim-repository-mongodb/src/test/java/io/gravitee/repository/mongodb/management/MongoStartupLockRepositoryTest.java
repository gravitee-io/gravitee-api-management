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
package io.gravitee.repository.mongodb.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.repository.exceptions.StartupLockTimeoutException;
import io.gravitee.repository.management.AbstractManagementRepositoryTest;
import io.gravitee.repository.management.api.StartupLock;
import io.gravitee.repository.management.api.StartupLockRepository;
import jakarta.inject.Inject;
import java.time.Duration;
import org.junit.Test;
import org.springframework.data.mongodb.core.MongoOperations;

public class MongoStartupLockRepositoryTest extends AbstractManagementRepositoryTest {

    private static final Duration SHORT_LEASE = Duration.ofSeconds(1);
    private static final Duration NEVER = Duration.ofHours(1);
    private static final Duration SHORT_WAIT = Duration.ofSeconds(1);
    private static final Duration LONGER_THAN_SHORT_LEASE = Duration.ofSeconds(3);

    @Inject
    private StartupLockRepository startupLockRepository;

    @Inject
    private MongoOperations mongoOperations;

    @Override
    protected String getTestCasesPath() {
        return null;
    }

    @Test
    public void should_acquire_the_lock_once_the_holder_stopped_renewing_its_lease() throws Exception {
        try (StartupLock ignored = new MongoStartupLockRepository(mongoOperations, SHORT_LEASE, NEVER).acquire(SHORT_WAIT)) {
            try (StartupLock next = startupLockRepository.acquire(Duration.ofSeconds(10))) {
                assertThat(next).isNotNull();
            }
        }
    }

    @Test
    public void should_keep_the_lock_while_the_holder_renews_its_lease() throws Exception {
        try (
            StartupLock ignored = new MongoStartupLockRepository(mongoOperations, SHORT_LEASE, Duration.ofMillis(200)).acquire(SHORT_WAIT)
        ) {
            assertThatThrownBy(() -> startupLockRepository.acquire(LONGER_THAN_SHORT_LEASE)).isInstanceOf(
                StartupLockTimeoutException.class
            );
        }
    }

    @Test
    public void should_not_release_the_lock_of_the_next_holder_when_an_expired_holder_closes() throws Exception {
        StartupLock expired = new MongoStartupLockRepository(mongoOperations, SHORT_LEASE, NEVER).acquire(SHORT_WAIT);
        try (StartupLock ignored = startupLockRepository.acquire(Duration.ofSeconds(10))) {
            expired.close();

            assertThatThrownBy(() -> startupLockRepository.acquire(SHORT_WAIT)).isInstanceOf(StartupLockTimeoutException.class);
        }
    }
}
