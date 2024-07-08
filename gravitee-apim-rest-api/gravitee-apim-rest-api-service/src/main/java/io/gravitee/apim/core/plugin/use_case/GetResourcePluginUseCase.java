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
package io.gravitee.apim.core.plugin.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.plugin.crud_service.ResourcePluginCrudService;
import io.gravitee.apim.core.plugin.domain_service.PluginFilterByLicenseDomainService;
import io.gravitee.apim.core.plugin.model.ResourcePlugin;
import io.gravitee.apim.core.plugin.query_service.ResourcePluginQueryService;
import io.gravitee.rest.api.service.exceptions.ResourceNotFoundException;
import java.util.Set;
import lombok.AllArgsConstructor;

@UseCase
@AllArgsConstructor
public class GetResourcePluginUseCase {

    private final ResourcePluginCrudService resourcePluginCrudService;
    private final ResourcePluginQueryService resourcePluginQueryService;
    private final PluginFilterByLicenseDomainService licenseChecker;

    public Output execute(Input input) {
        var resourcePlugin = resourcePluginCrudService
            .get(input.resourceId)
            .flatMap(resource ->
                licenseChecker.setPluginDeployedStatusDependingOnLicense(Set.of(resource), input.organizationId).stream().findFirst()
            )
            .orElseThrow(() -> new ResourceNotFoundException(input.resourceId));

        if (!input.withSchema) {
            return new Output(resourcePlugin, null);
        }

        var schema = resourcePluginQueryService.getSchema(input.resourceId);
        return new Output(resourcePlugin, schema);
    }

    public record Input(String organizationId, String resourceId, boolean withSchema) {
        public Input(String organizationId, String resourceId) {
            this(organizationId, resourceId, true);
        }
    }

    public record Output(ResourcePlugin plugin, String schema) {}
}
