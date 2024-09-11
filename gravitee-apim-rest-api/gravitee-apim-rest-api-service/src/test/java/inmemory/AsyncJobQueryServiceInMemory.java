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
package inmemory;

import io.gravitee.apim.core.async_job.model.AsyncJob;
import io.gravitee.apim.core.async_job.query_service.AsyncJobQueryService;
import io.gravitee.apim.core.integration.model.Integration;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.common.Pageable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class AsyncJobQueryServiceInMemory implements AsyncJobQueryService, InMemoryAlternative<AsyncJob> {

    private final List<AsyncJob> storage;

    public AsyncJobQueryServiceInMemory() {
        storage = new ArrayList<>();
    }

    public AsyncJobQueryServiceInMemory(AsyncJobCrudServiceInMemory asyncJobCrudServiceInMemory) {
        storage = asyncJobCrudServiceInMemory.storage;
    }

    @Override
    public Optional<AsyncJob> findPendingJobFor(String integrationId) {
        return storage
            .stream()
            .filter(integration -> integration.getSourceId().equals(integrationId))
            .filter(integration -> integration.getStatus().equals(AsyncJob.Status.PENDING))
            .findFirst();
    }

    @Override
    public Page<AsyncJob> listAsyncJobs(ListQuery query, Pageable pageable) {
        var pageNumber = pageable.getPageNumber();
        var pageSize = pageable.getPageSize();

        var matches = storage
            .stream()
            .filter(job -> query.initiatorId().map(value -> job.getInitiatorId().equals(value)).orElse(true))
            .filter(job -> query.type().map(value -> job.getType().equals(value)).orElse(true))
            .filter(job -> query.status().map(value -> job.getStatus().equals(value)).orElse(true))
            .sorted(Comparator.comparing(AsyncJob::getCreatedAt).reversed())
            .toList();

        var page = matches.size() <= pageSize
            ? matches
            : matches.subList((pageNumber - 1) * pageSize, Math.min(pageNumber * pageSize, matches.size()));

        return new Page<>(page, pageNumber, pageSize, matches.size());
    }

    @Override
    public void initWith(List<AsyncJob> items) {
        storage.clear();
        storage.addAll(items);
    }

    @Override
    public void reset() {
        storage.clear();
    }

    @Override
    public List<AsyncJob> storage() {
        return Collections.unmodifiableList(storage);
    }
}
