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
package io.gravitee.gamma.authorization.infra.service_provider;

import io.gravitee.apim.core.async_job.crud_service.AsyncJobCrudService;
import io.gravitee.apim.core.async_job.model.AsyncJob;
import io.gravitee.apim.plugin.gamma.api.identity.AmConnection;
import io.gravitee.gamma.authorization.api.AuthzCallerContext;
import io.gravitee.gamma.authorization.core.am.service_provider.AmUserSyncRunner;
import io.gravitee.gamma.authorization.core.am.use_case.SyncAmUsersUseCase;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.CustomLog;
import org.springframework.beans.factory.DisposableBean;

/**
 * Runs the AM user sync on a virtual-thread-per-task executor and records the outcome on the
 * persisted {@link AsyncJob}: SUCCESS with the synced count, or ERROR with the failure message. The
 * work runs in-process (there is no external agent for it), but its lifecycle lives in the AsyncJob
 * store.
 */
@CustomLog
public class AmUserSyncRunnerImpl implements AmUserSyncRunner, DisposableBean {

    private final SyncAmUsersUseCase syncAmUsersUseCase;
    private final AsyncJobCrudService asyncJobCrudService;
    private final ExecutorService executor;

    public AmUserSyncRunnerImpl(SyncAmUsersUseCase syncAmUsersUseCase, AsyncJobCrudService asyncJobCrudService) {
        this(
            syncAmUsersUseCase,
            asyncJobCrudService,
            // A sync run is one long, mostly IO-bound task (paging the AM API, upserting entities), so a
            // virtual thread per task fits better than a pooled platform thread — it parks cheaply while
            // waiting on AM. Virtual threads are daemon, so they don't pin the JVM on shutdown either.
            Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("am-user-sync-", 0).factory())
        );
    }

    // Test seam: lets a test inject a controllable executor.
    AmUserSyncRunnerImpl(SyncAmUsersUseCase syncAmUsersUseCase, AsyncJobCrudService asyncJobCrudService, ExecutorService executor) {
        this.syncAmUsersUseCase = syncAmUsersUseCase;
        this.asyncJobCrudService = asyncJobCrudService;
        this.executor = executor;
    }

    @Override
    public void runAsync(AsyncJob job, AuthzCallerContext caller, AmConnection connection) {
        executor.submit(() -> run(job, caller, connection));
    }

    @Override
    public void destroy() {
        // Stop the worker pool on context close / plugin unload so its threads don't pin the
        // plugin classloader. Daemon threads + in-flight syncs are left to finish on their own.
        executor.shutdown();
    }

    private void run(AsyncJob job, AuthzCallerContext caller, AmConnection connection) {
        try {
            SyncAmUsersUseCase.Output output = syncAmUsersUseCase.execute(new SyncAmUsersUseCase.Input(caller, connection));
            asyncJobCrudService.update(job.toBuilder().upperLimit((long) output.entitiesUpserted()).build().complete());
        } catch (Throwable e) {
            // Catch Throwable, not just RuntimeException: an Error (e.g. NoClassDefFoundError from the
            // AM SDK) would otherwise escape this worker, be swallowed by the thread pool, and strand
            // the job at PENDING until its deadline.
            log.error("AM user sync failed for organization {} (job {})", job.getSourceId(), job.getId(), e);
            asyncJobCrudService.update(job.error(e.getMessage()));
        }
    }
}
