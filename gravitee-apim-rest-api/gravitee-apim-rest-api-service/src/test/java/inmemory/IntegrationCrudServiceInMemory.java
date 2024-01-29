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

import io.gravitee.apim.core.integration.crud_service.IntegrationCrudService;
import io.gravitee.apim.core.integration.exception.IntegrationNotFoundException;
import io.gravitee.apim.core.integration.model.IntegrationEntity;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Remi Baptiste (remi.baptiste at graviteesource.com)
 * @author GraviteeSource Team
 */
public class IntegrationCrudServiceInMemory implements IntegrationCrudService, InMemoryAlternative<IntegrationEntity> {

    private final List<IntegrationEntity> storage = new ArrayList<>();

    @Override
    public IntegrationEntity create(IntegrationEntity integration) {
        storage.add(integration);
        return integration;
    }

    @Override
    public IntegrationEntity findById(String integrationId) {
        if (integrationId == null) {
            throw new TechnicalManagementException("integrationId should not be null");
        }
        return storage
            .stream()
            .filter(integrationEntity -> integrationEntity.getId().equals(integrationId))
            .findFirst()
            .orElseThrow(() -> new IntegrationNotFoundException(integrationId));
    }

    @Override
    public Set<IntegrationEntity> findAll() {
        return new HashSet<>(storage);
    }

    @Override
    public Set<IntegrationEntity> findByEnvironment(String environmentId) {
        return storage
            .stream()
            .filter(integrationEntity -> integrationEntity.getEnvironmentId().equals(environmentId))
            .collect(Collectors.toSet());
    }

    @Override
    public void delete(String integrationId) {
        OptionalInt index = this.findIndex(this.storage, integration -> integration.getId().equals(integrationId));
        if (index.isPresent()) {
            storage.remove(index.getAsInt());
        }
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
