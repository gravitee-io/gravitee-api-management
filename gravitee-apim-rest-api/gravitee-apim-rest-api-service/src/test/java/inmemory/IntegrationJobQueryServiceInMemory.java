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

import io.gravitee.apim.core.integration.model.IntegrationJob;
import io.gravitee.apim.core.integration.query_service.IntegrationJobQueryService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class IntegrationJobQueryServiceInMemory implements IntegrationJobQueryService, InMemoryAlternative<IntegrationJob> {

    private final List<IntegrationJob> storage;

    public IntegrationJobQueryServiceInMemory() {
        storage = new ArrayList<>();
    }

    public IntegrationJobQueryServiceInMemory(IntegrationJobCrudServiceInMemory integrationJobCrudServiceInMemory) {
        storage = integrationJobCrudServiceInMemory.storage;
    }

    @Override
    public Optional<IntegrationJob> findPendingJobFor(String integrationId) {
        return storage
            .stream()
            .filter(integration -> integration.getSourceId().equals(integrationId))
            .filter(integration -> integration.getStatus().equals(IntegrationJob.Status.PENDING))
            .findFirst();
    }

    @Override
    public void initWith(List<IntegrationJob> items) {
        storage.clear();
        storage.addAll(items);
    }

    @Override
    public void reset() {
        storage.clear();
    }

    @Override
    public List<IntegrationJob> storage() {
        return Collections.unmodifiableList(storage);
    }
}
