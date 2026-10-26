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
package io.gravitee.repository.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.repository.exceptions.StartupLockTimeoutException;
import io.gravitee.repository.management.api.StartupLock;
import io.gravitee.repository.management.api.StartupLockRepository;
import io.gravitee.repository.management.model.Role;
import io.gravitee.repository.management.model.RoleReferenceType;
import io.gravitee.repository.management.model.RoleScope;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class StartupLockRepositoryTest extends AbstractManagementRepositoryTest {

    private static final Duration LONG_WAIT = Duration.ofSeconds(60);
    private static final Duration SHORT_WAIT = Duration.ofSeconds(1);
    private static final long OVERLAP_WINDOW_MILLIS = 100;

    @Inject
    private StartupLockRepository startupLockRepository;

    private ExecutorService executor;

    @Before
    public void createExecutor() {
        executor = Executors.newCachedThreadPool();
    }

    @After
    public void shutdownExecutor() {
        executor.shutdownNow();
    }

    @Override
    protected String getTestCasesPath() {
        return null;
    }

    @Test
    public void should_acquire_the_lock_when_it_is_free() throws Exception {
        try (StartupLock lock = startupLockRepository.acquire(SHORT_WAIT)) {
            assertThat(lock).isNotNull();
        }
    }

    @Test
    public void should_time_out_while_another_holder_holds_the_lock() throws Exception {
        try (StartupLock ignored = startupLockRepository.acquire(SHORT_WAIT)) {
            assertThatThrownBy(() -> startupLockRepository.acquire(SHORT_WAIT)).isInstanceOf(StartupLockTimeoutException.class);
        }
    }

    @Test
    public void should_acquire_the_lock_once_the_previous_holder_closed_it() throws Exception {
        StartupLock first = startupLockRepository.acquire(SHORT_WAIT);
        first.close();

        try (StartupLock second = startupLockRepository.acquire(SHORT_WAIT)) {
            assertThat(second).isNotNull();
        }
    }

    @Test
    public void should_grant_the_lock_to_one_contender_at_a_time() throws Exception {
        int contenders = 8;
        AtomicInteger holders = new AtomicInteger();
        AtomicInteger maxHolders = new AtomicInteger();
        AtomicInteger completed = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < contenders; i++) {
            futures.add(
                executor.submit(() -> {
                    start.await();
                    try (StartupLock ignored = startupLockRepository.acquire(LONG_WAIT)) {
                        maxHolders.accumulateAndGet(holders.incrementAndGet(), Math::max);
                        Thread.sleep(OVERLAP_WINDOW_MILLIS);
                        holders.decrementAndGet();
                    }
                    completed.incrementAndGet();
                    return null;
                })
            );
        }
        start.countDown();
        for (Future<?> future : futures) {
            future.get(120, TimeUnit.SECONDS);
        }

        assertThat(maxHolders.get()).isEqualTo(1);
        assertThat(completed.get()).isEqualTo(contenders);
    }

    @Test
    public void should_not_block_writes_while_the_lock_is_held() throws Exception {
        try (StartupLock ignored = startupLockRepository.acquire(SHORT_WAIT)) {
            Future<Optional<Role>> written = executor.submit(() -> {
                roleRepository.create(aRole("role-written-while-locked"));
                return roleRepository.findById("role-written-while-locked");
            });

            assertThat(written.get(10, TimeUnit.SECONDS)).map(Role::getName).contains("written while locked");
        }
    }

    @Test
    public void should_ignore_a_second_close() throws Exception {
        StartupLock lock = startupLockRepository.acquire(SHORT_WAIT);
        lock.close();

        assertThatCode(lock::close).doesNotThrowAnyException();
    }

    private static Role aRole(String id) {
        Role role = new Role();
        role.setId(id);
        role.setName("written while locked");
        role.setScope(RoleScope.API);
        role.setReferenceId("DEFAULT");
        role.setReferenceType(RoleReferenceType.ORGANIZATION);
        role.setPermissions(new int[] { 3 });
        return role;
    }
}
