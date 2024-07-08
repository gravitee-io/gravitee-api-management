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

import io.gravitee.apim.core.plugin.crud_service.ResourcePluginCrudService;
import io.gravitee.apim.core.plugin.model.ResourcePlugin;
import io.gravitee.rest.api.service.exceptions.ResourceNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ResourcePluginCrudServiceInMemory implements ResourcePluginCrudService, InMemoryAlternative<ResourcePlugin> {

    final List<ResourcePlugin> storage = new ArrayList<>();

    @Override
    public Optional<ResourcePlugin> get(String resourceId) {
        if (resourceId == null) {
            throw new TechnicalManagementException("resourceId should not be null");
        }

        var resource = storage.stream().filter(resourcePlugin -> resourceId.equals(resourcePlugin.getId())).findFirst();

        if (resource.isEmpty()) {
            throw new ResourceNotFoundException(resourceId);
        }
        return resource;
    }

    @Override
    public void initWith(List<ResourcePlugin> items) {
        storage.addAll(items.stream().toList());
    }

    @Override
    public void reset() {
        storage.clear();
    }

    @Override
    public List<ResourcePlugin> storage() {
        return Collections.unmodifiableList(storage);
    }
}
