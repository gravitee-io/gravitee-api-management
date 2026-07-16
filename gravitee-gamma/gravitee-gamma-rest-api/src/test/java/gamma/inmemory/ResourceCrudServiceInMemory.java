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
package gamma.inmemory;

import io.gravitee.gamma.core.domain.resource.crud_service.ResourceCrudService;
import io.gravitee.gamma.core.domain.resource.model.Resource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class ResourceCrudServiceInMemory implements ResourceCrudService {

    final Map<String, Resource> storage = new LinkedHashMap<>();

    public void reset() {
        storage.clear();
    }

    public void initWith(List<Resource> resources) {
        storage.clear();
        resources.forEach(r -> storage.put(r.id(), r));
    }

    public List<Resource> storage() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public Resource create(Resource resource) {
        if (storage.containsKey(resource.id())) {
            throw new IllegalStateException("Resource [" + resource.id() + "] already exists.");
        }
        storage.put(resource.id(), resource);
        return resource;
    }

    @Override
    public Resource update(Resource resource) {
        if (!storage.containsKey(resource.id())) {
            throw new IllegalStateException("Resource [" + resource.id() + "] does not exist.");
        }
        storage.put(resource.id(), resource);
        return resource;
    }

    @Override
    public Optional<Resource> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public void delete(String id) {
        storage.remove(id);
    }

    @Override
    public boolean existsByNameAndReference(String name, Resource.ReferenceType referenceType, String referenceId) {
        return storage
            .values()
            .stream()
            .anyMatch(
                r ->
                    r.referenceType() == referenceType &&
                    Objects.equals(r.referenceId(), referenceId) &&
                    r.definition() != null &&
                    Objects.equals(r.definition().getName(), name)
            );
    }
}
