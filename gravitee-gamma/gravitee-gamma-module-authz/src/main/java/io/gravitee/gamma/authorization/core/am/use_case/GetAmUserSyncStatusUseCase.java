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

import io.gravitee.apim.core.async_job.model.AsyncJob;
import io.gravitee.apim.core.async_job.query_service.AsyncJobQueryService;
import io.gravitee.rest.api.model.common.PageableImpl;
import java.util.Optional;

/**
 * Returns the most recent AM user-sync job for an organization, or empty when none has run.
 * Backed by {@link AsyncJobQueryService}, which orders by {@code updatedAt} descending and
 * auto-transitions late jobs to TIMEOUT.
 */
public class GetAmUserSyncStatusUseCase {

    private final AsyncJobQueryService asyncJobQueryService;

    public GetAmUserSyncStatusUseCase(AsyncJobQueryService asyncJobQueryService) {
        this.asyncJobQueryService = asyncJobQueryService;
    }

    public record Input(String organizationId, String environmentId) {}

    public record Output(Optional<AsyncJob> job) {}

    public Output execute(Input input) {
        var query = new AsyncJobQueryService.ListQuery(
            input.environmentId(),
            Optional.empty(),
            Optional.of(AsyncJob.Type.AM_USER_SYNC),
            Optional.empty(),
            Optional.of(input.organizationId())
        );
        Optional<AsyncJob> latest = asyncJobQueryService.listAsyncJobs(query, new PageableImpl(1, 1)).getContent().stream().findFirst();
        return new Output(latest);
    }
}
