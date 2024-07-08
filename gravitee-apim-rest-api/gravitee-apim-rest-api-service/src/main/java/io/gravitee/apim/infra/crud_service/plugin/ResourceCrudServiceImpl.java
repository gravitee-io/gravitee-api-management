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
package io.gravitee.apim.infra.crud_service.plugin;

import io.gravitee.apim.core.plugin.crud_service.ResourcePluginCrudService;
import io.gravitee.apim.core.plugin.model.ResourcePlugin;
import io.gravitee.apim.infra.adapter.ResourcePluginAdapter;
import io.gravitee.rest.api.service.ResourceService;
import io.gravitee.rest.api.service.exceptions.PluginNotFoundException;
import io.gravitee.rest.api.service.exceptions.ResourceNotFoundException;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ResourceCrudServiceImpl implements ResourcePluginCrudService {

    private final ResourceService resourceService;

    public ResourceCrudServiceImpl(@Lazy ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @Override
    public Optional<ResourcePlugin> get(String resourceId) {
        log.debug("Find resource by id : {}", resourceId);

        try {
            return Optional.of(ResourcePluginAdapter.INSTANCE.map(resourceService.findById(resourceId)));
        } catch (PluginNotFoundException e) {
            throw new ResourceNotFoundException(resourceId);
        }
    }
}
