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

import io.gravitee.apim.core.integration.model.IntegrationEntity;
import io.gravitee.apim.core.integration.query_service.IntegrationQueryService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author Remi Baptiste (remi.baptiste at graviteesource.com)
 * @author GraviteeSource Team
 */
public class IntegrationQueryServiceInMemory implements IntegrationQueryService, InMemoryAlternative<IntegrationEntity> {

    private final List<IntegrationEntity> storage = new ArrayList<>();

    @Override
    public Optional<IntegrationEntity> findByEnvironmentIdAndRemoteId(String environmentId, String remoteId) {
        return storage
            .stream()
            .filter(integrationEntity ->
                integrationEntity.getEnvironmentId().equals(environmentId) && integrationEntity.getRemoteId().equals(remoteId)
            )
            .findFirst();
    }

    @Override
    public void initWith(List<IntegrationEntity> items) {
        reset();
        storage.addAll(items);
    }

    @Override
    public void reset() {
        storage.clear();
    }

    @Override
    public List<IntegrationEntity> storage() {
        return storage;
    }
}
