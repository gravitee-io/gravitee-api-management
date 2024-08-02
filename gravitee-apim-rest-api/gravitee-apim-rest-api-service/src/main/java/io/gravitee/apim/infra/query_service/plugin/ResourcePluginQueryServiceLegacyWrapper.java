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
package io.gravitee.apim.infra.query_service.plugin;

import io.gravitee.apim.core.plugin.model.ResourcePlugin;
import io.gravitee.apim.core.plugin.query_service.ResourcePluginQueryService;
import io.gravitee.apim.infra.adapter.ResourcePluginAdapter;
import io.gravitee.rest.api.service.ResourceService;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ResourcePluginQueryServiceLegacyWrapper implements ResourcePluginQueryService {

    private final ResourceService resourceService;

    public ResourcePluginQueryServiceLegacyWrapper(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @Override
    public Set<ResourcePlugin> findAll() {
        return resourceService.findAll().stream().map(ResourcePluginAdapter.INSTANCE::map).collect(Collectors.toSet());
    }

    @Override
    public String getSchema(String resourceId) {
        return resourceService.getSchema(resourceId);
    }
}
