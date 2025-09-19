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
import io.gravitee.apim.core.integration.model.Integration;
import java.util.*;

public class IntegrationCrudServiceInMemory implements IntegrationCrudService, InMemoryAlternative<Integration> {

    final ArrayList<Integration> storage = new ArrayList<>();

    @Override
    public Integration create(Integration integration) {
        storage.add(integration);
        return integration;
    }

    @Override
    public Optional<Integration.ApiIntegration> findApiIntegrationById(String id) {
        return findById(id).flatMap(integration ->
            integration instanceof Integration.ApiIntegration apiIntegration ? Optional.of(apiIntegration) : Optional.empty()
        );
    }

    @Override
    public Optional<Integration.A2aIntegration> findA2aIntegrationById(String id) {
        return findById(id).flatMap(integration ->
            integration instanceof Integration.A2aIntegration a2aIntegration ? Optional.of(a2aIntegration) : Optional.empty()
        );
    }

    @Override
    public Optional<Integration> findById(String id) {
        return storage
            .stream()
            .filter(item -> item.id().equals(id))
            .findFirst();
    }

    @Override
    public Integration update(Integration integration) {
        var index = findIndex(storage, i -> i.id().equals(integration.id())).orElseThrow(() ->
            new IllegalStateException("Integration not found")
        );
        storage.set(index, integration);
        return integration;
    }

    @Override
    public void delete(String id) {
        findIndex(storage, i -> i.id().equals(id)).ifPresent(storage::remove);
    }

    @Override
    public void initWith(List<Integration> items) {
        storage.clear();
        storage.addAll(items);
    }

    public <T extends Integration> void initializeWith(List<T> items) {
        storage.clear();
        storage.addAll(items);
    }

    @Override
    public void reset() {
        storage.clear();
    }

    @Override
    public List<Integration> storage() {
        return Collections.unmodifiableList(storage);
    }
}
