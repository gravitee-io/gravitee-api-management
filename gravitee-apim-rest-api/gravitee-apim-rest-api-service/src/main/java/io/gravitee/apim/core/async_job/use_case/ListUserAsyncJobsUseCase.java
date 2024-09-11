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
package io.gravitee.apim.core.async_job.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.async_job.model.AsyncJob;
import io.gravitee.apim.core.async_job.query_service.AsyncJobQueryService;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.common.PageableImpl;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@UseCase
public class ListUserAsyncJobsUseCase {

    private final AsyncJobQueryService asyncJobQueryService;

    public Output execute(Input input) {
        var pageable = input.pageable.orElse(new PageableImpl(1, 10));
        var page = asyncJobQueryService.listAsyncJobs(
            new AsyncJobQueryService.ListQuery(
                input.environmentId,
                Optional.of(input.initiatorId),
                input.type,
                input.status,
                input.sourceId
            ),
            pageable
        );
        return new Output(page);
    }

    public record Input(
        String environmentId,
        String initiatorId,
        Optional<AsyncJob.Type> type,
        Optional<AsyncJob.Status> status,
        Optional<String> sourceId,
        Optional<Pageable> pageable
    ) {
        public Input {
            if (environmentId == null) {
                throw new IllegalArgumentException("Environment ID is required");
            }
            if (initiatorId == null) {
                throw new IllegalArgumentException("Initiator ID is required");
            }
        }

        public Input(String environmentId, String initiatorId) {
            this(environmentId, initiatorId, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        }

        public Input(String environmentId, String initiatorId, AsyncJob.Type type) {
            this(environmentId, initiatorId, Optional.ofNullable(type), Optional.empty(), Optional.empty(), Optional.empty());
        }

        public Input(String environmentId, String initiatorId, AsyncJob.Status status) {
            this(environmentId, initiatorId, Optional.empty(), Optional.ofNullable(status), Optional.empty(), Optional.empty());
        }

        public Input(String environmentId, String initiatorId, AsyncJob.Type type, AsyncJob.Status status) {
            this(environmentId, initiatorId, Optional.ofNullable(type), Optional.ofNullable(status), Optional.empty(), Optional.empty());
        }
    }

    public record Output(Page<AsyncJob> jobs) {}
}
