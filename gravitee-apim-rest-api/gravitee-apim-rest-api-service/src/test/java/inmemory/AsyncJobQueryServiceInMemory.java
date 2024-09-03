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

import io.gravitee.apim.core.integration.model.AsyncJob;
import io.gravitee.apim.core.integration.query_service.AsyncJobQueryService;
import java.util.ArrayList;
import java.util.Collections;
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
