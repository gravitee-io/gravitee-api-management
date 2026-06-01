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
import io.gravitee.gamma.authorization.core.am.service_provider.AmDirectoryClient;
import io.gravitee.gamma.authorization.core.am.service_provider.AmUserSyncRunner;
import io.gravitee.rest.api.model.common.PageableImpl;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Starts an asynchronous AM user sync for the caller's organization and environment. Persists the
 * job as an {@link AsyncJob} (PENDING) so its lifecycle survives restarts and is queryable, then
 * hands the work to {@link AmUserSyncRunner}. Rejects a start when one is already running for the
 * same org + environment (the scope the sync writes into and that the status endpoint reports on).
 *
 * @author GraviteeSource Team
 */
public class StartAmUserSyncUseCase {

    private static final Duration JOB_DEADLINE = Duration.ofMinutes(30);

    private final AsyncJobQueryService queryService;
    private final AsyncJobCrudService crudService;
    private final AmConnectionRepository connectionRepository;
    private final AmDirectoryClient directoryClient;
    private final AmUserSyncRunner syncRunner;

    public StartAmUserSyncUseCase(
        AsyncJobQueryService queryService,
        AsyncJobCrudService crudService,
        AmConnectionRepository connectionRepository,
        AmDirectoryClient directoryClient,
        AmUserSyncRunner syncRunner
    ) {
        this.queryService = queryService;
        this.crudService = crudService;
        this.connectionRepository = connectionRepository;
        this.directoryClient = directoryClient;
        this.syncRunner = syncRunner;
    }


    public Output execute(Input input) {
        AuthzCallerContext caller = input.caller();
        String organizationId = caller.organizationId();

        // Scope the conflict check to org + environment + AM_USER_SYNC
        var pendingQuery = new AsyncJobQueryService.ListQuery(
            caller.environmentId(),
            Optional.empty(),
            Optional.of(AsyncJob.Type.AM_USER_SYNC),
            Optional.of(AsyncJob.Status.PENDING),
            Optional.of(organizationId)
        );

        boolean alreadyRunning = queryService
            .listAsyncJobs(pendingQuery, new PageableImpl(1, 1))
            .getContent()
            .stream()
            .anyMatch(job -> !job.isTimedOut());

        if (alreadyRunning) {
            throw new AmSyncConflictException(organizationId);
        }

        AmConnection connection = connectionRepository.requireByOrg(organizationId);

        long totalUsers;
        try (AmDirectoryClient.Session session = directoryClient.openSession(connection)) {
            totalUsers = session.fetchUsers(0, 1).totalCount();
        }

        ZonedDateTime now = TimeProvider.now();
        AsyncJob job = crudService.create(
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

        syncRunner.runAsync(job, caller, connection);
        return new Output(job, totalUsers);
    }

    public record Input(AuthzCallerContext caller) {}

    public record Output(AsyncJob job, long totalUsers) {}
}
