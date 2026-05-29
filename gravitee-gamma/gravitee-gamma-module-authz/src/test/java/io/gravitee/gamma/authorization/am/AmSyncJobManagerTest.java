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
package io.gravitee.gamma.authorization.am;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.plugin.gamma.api.identity.AmConnection;
import io.gravitee.gamma.authorization.api.AuthzCallerContext;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AmSyncJobManagerTest {

    private static final AuthzCallerContext CALLER = AuthzCallerContext.ofUser("org-1", "env-1", "user-1");
    private static final AmConnection CONNECTION = new AmConnection("http://am:8093", "token", "domain-1", "domain-hrid", null);

    private AmUserSyncOrchestrator orchestrator;
    private AmSyncJobManager manager;

    @BeforeEach
    void setUp() {
        orchestrator = mock(AmUserSyncOrchestrator.class);
        manager = new AmSyncJobManager(orchestrator);
    }

    private AmSyncJobState awaitTerminal(String organizationId) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 2_000;
        while (System.currentTimeMillis() < deadline) {
            AmSyncJobState state = manager.getStatus(organizationId).orElseThrow();
            if (state.status() != AmSyncStatus.RUNNING) {
                return state;
            }
            Thread.sleep(10);
        }
        throw new AssertionError("job did not reach a terminal state in time");
    }

    @Test
    void start_returns_a_running_job_and_completes_with_counts() throws InterruptedException {
        when(orchestrator.run(any(), any())).thenReturn(new AmUserSyncOrchestrator.Result(5, 7));

        AmSyncJobState started = manager.start(CALLER, CONNECTION);

        assertThat(started.status()).isEqualTo(AmSyncStatus.RUNNING);
        assertThat(started.jobId()).isNotBlank();

        AmSyncJobState terminal = awaitTerminal("org-1");
        assertThat(terminal.status()).isEqualTo(AmSyncStatus.COMPLETED);
        assertThat(terminal.jobId()).isEqualTo(started.jobId());
        assertThat(terminal.usersFetched()).isEqualTo(5);
        assertThat(terminal.entitiesUpserted()).isEqualTo(7);
        assertThat(terminal.completedAt()).isNotNull();
        assertThat(terminal.error()).isNull();
    }

    @Test
    void records_failure_when_the_orchestrator_throws() throws InterruptedException {
        when(orchestrator.run(any(), any())).thenThrow(new RuntimeException("upstream down"));

        manager.start(CALLER, CONNECTION);

        AmSyncJobState terminal = awaitTerminal("org-1");
        assertThat(terminal.status()).isEqualTo(AmSyncStatus.FAILED);
        assertThat(terminal.error()).isEqualTo("upstream down");
        assertThat(terminal.completedAt()).isNotNull();
    }

    @Test
    void rejects_a_concurrent_start_for_the_same_organization() throws InterruptedException {
        CountDownLatch release = new CountDownLatch(1);
        when(orchestrator.run(any(), any())).thenAnswer(invocation -> {
            release.await();
            return new AmUserSyncOrchestrator.Result(0, 0);
        });

        manager.start(CALLER, CONNECTION);

        assertThatThrownBy(() -> manager.start(CALLER, CONNECTION)).isInstanceOf(AmSyncConflictException.class);

        release.countDown();
        assertThat(awaitTerminal("org-1").status()).isEqualTo(AmSyncStatus.COMPLETED);
    }

    @Test
    void getStatus_is_empty_when_no_job_has_run() {
        assertThat(manager.getStatus("never-synced")).isEmpty();
    }
}
