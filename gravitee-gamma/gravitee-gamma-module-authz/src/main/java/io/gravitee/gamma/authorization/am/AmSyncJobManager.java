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

import io.gravitee.apim.plugin.gamma.api.identity.AmConnection;
import io.gravitee.gamma.authorization.api.AuthzCallerContext;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.CustomLog;

/**
 * Runs AM user-sync jobs asynchronously, one at a time per organization. Job state is held in
 * memory (lost on restart) keyed by organization id. A start request for an organization that
 * already has a RUNNING job is rejected with {@link AmSyncConflictException}.
 */
@CustomLog
public class AmSyncJobManager {

    private final AmUserSyncOrchestrator orchestrator;
    private final ExecutorService executor;
    private final ConcurrentHashMap<String, AmSyncJobState> jobs = new ConcurrentHashMap<>();

    public AmSyncJobManager(AmUserSyncOrchestrator orchestrator) {
        this(
            orchestrator,
            Executors.newCachedThreadPool(runnable -> {
                Thread thread = new Thread(runnable, "am-user-sync");
                thread.setDaemon(true);
                return thread;
            })
        );
    }

    // Test seam: lets a test inject a controllable executor.
    AmSyncJobManager(AmUserSyncOrchestrator orchestrator, ExecutorService executor) {
        this.orchestrator = orchestrator;
        this.executor = executor;
    }

    public AmSyncJobState start(AuthzCallerContext caller, AmConnection connection) {
        String organizationId = caller.organizationId();
        AmSyncJobState running = jobs.compute(organizationId, (org, existing) -> {
            if (existing != null && existing.status() == AmSyncStatus.RUNNING) {
                throw new AmSyncConflictException(org);
            }
            return AmSyncJobState.running(UUID.randomUUID().toString(), Instant.now());
        });
        executor.submit(() -> runSync(organizationId, running, caller, connection));
        return running;
    }

    public Optional<AmSyncJobState> getStatus(String organizationId) {
        return Optional.ofNullable(jobs.get(organizationId));
    }

    // The single-RUNNING-per-org invariant guarantees this job owns the slot until it finishes,
    // so a plain put is safe — no concurrent writer can be present.
    private void runSync(String organizationId, AmSyncJobState running, AuthzCallerContext caller, AmConnection connection) {
        try {
            AmUserSyncOrchestrator.Result result = orchestrator.run(caller, connection);
            jobs.put(organizationId, running.completed(result.usersFetched(), result.entitiesUpserted(), Instant.now()));
        } catch (RuntimeException e) {
            log.warn("AM user sync failed for organization {}", organizationId, e);
            jobs.put(organizationId, running.failed(e.getMessage(), Instant.now()));
        }
    }
}
