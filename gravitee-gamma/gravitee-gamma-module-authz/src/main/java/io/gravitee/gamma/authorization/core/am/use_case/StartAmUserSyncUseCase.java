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
package io.gravitee.gamma.authorization.core.am.use_case;

import io.gravitee.apim.core.async_job.crud_service.AsyncJobCrudService;
import io.gravitee.apim.core.async_job.model.AsyncJob;
import io.gravitee.apim.core.async_job.query_service.AsyncJobQueryService;
import io.gravitee.apim.plugin.gamma.api.identity.AmConnection;
import io.gravitee.apim.plugin.gamma.api.identity.AmConnectionRepository;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.gamma.authorization.api.AuthzCallerContext;
import io.gravitee.gamma.authorization.core.am.exception.AmSyncConflictException;
import io.gravitee.gamma.authorization.core.am.service_provider.AmUserSyncRunner;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Starts an asynchronous AM user sync for the caller's organization. Persists the job as an
 * {@link AsyncJob} (PENDING) so its lifecycle survives restarts and is queryable, then hands the
 * work to {@link AmUserSyncRunner}. Rejects a start when one is already running for the org.
 */
public class StartAmUserSyncUseCase {

    // Bounds a stranded job (e.g. a node that died mid-sync): once past the deadline the persisted
    // job auto-transitions to TIMEOUT on the next query, so it stops blocking new syncs.
    private static final Duration JOB_DEADLINE = Duration.ofMinutes(30);

    private final AsyncJobQueryService asyncJobQueryService;
    private final AsyncJobCrudService asyncJobCrudService;
    private final AmConnectionRepository amConnectionRepository;
    private final AmUserSyncRunner runner;

    public StartAmUserSyncUseCase(
        AsyncJobQueryService asyncJobQueryService,
        AsyncJobCrudService asyncJobCrudService,
        AmConnectionRepository amConnectionRepository,
        AmUserSyncRunner runner
    ) {
        this.asyncJobQueryService = asyncJobQueryService;
        this.asyncJobCrudService = asyncJobCrudService;
        this.amConnectionRepository = amConnectionRepository;
        this.runner = runner;
    }

    public record Input(AuthzCallerContext caller) {}

    public record Output(AsyncJob job) {}

    public Output execute(Input input) {
        AuthzCallerContext caller = input.caller();
        String organizationId = caller.organizationId();

        if (asyncJobQueryService.findPendingJobFor(organizationId).isPresent()) {
            throw new AmSyncConflictException(organizationId);
        }
        // Throws AmNotConfiguredException when no AM connection is configured for the org.
        AmConnection connection = amConnectionRepository.requireByOrg(organizationId);

        ZonedDateTime now = TimeProvider.now();
        AsyncJob job = asyncJobCrudService.create(
            AsyncJob
                .builder()
                .id(UUID.randomUUID().toString())
                .sourceId(organizationId)
                .environmentId(caller.environmentId())
                .initiatorId(caller.userId())
                .type(AsyncJob.Type.AM_USER_SYNC)
                .status(AsyncJob.Status.PENDING)
                .createdAt(now)
                .updatedAt(now)
                .deadLine(now.plus(JOB_DEADLINE))
                .build()
        );

        runner.runAsync(job, caller, connection);
        return new Output(job);
    }
}
