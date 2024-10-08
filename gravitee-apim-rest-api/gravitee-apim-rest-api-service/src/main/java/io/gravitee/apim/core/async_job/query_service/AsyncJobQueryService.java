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
package io.gravitee.apim.core.async_job.query_service;

import io.gravitee.apim.core.async_job.model.AsyncJob;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.common.Pageable;
import java.util.Optional;

public interface AsyncJobQueryService {
    Optional<AsyncJob> findPendingJobFor(String integrationId);

    Page<AsyncJob> listAsyncJobs(ListQuery query, Pageable pageable);

    record ListQuery(
        String environmentId,
        Optional<String> initiatorId,
        Optional<AsyncJob.Type> type,
        Optional<AsyncJob.Status> status,
        Optional<String> sourceId
    ) {
        public ListQuery(String environmentId) {
            this(environmentId, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        }
    }
}
