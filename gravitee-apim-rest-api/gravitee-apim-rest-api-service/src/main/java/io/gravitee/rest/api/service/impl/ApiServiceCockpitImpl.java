/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.rest.api.model.ImportSwaggerDescriptorEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.api.SwaggerApiEntity;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.ApiServiceCockpit;
import io.gravitee.rest.api.service.PageService;
import io.gravitee.rest.api.service.SwaggerService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * @author Julien GIOVARESCO (julien.giovaresco at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class ApiServiceCockpitImpl implements ApiServiceCockpit {

    private final ObjectMapper objectMapper;
    private final ApiService apiService;
    private final SwaggerService swaggerService;
    private final PageService pageService;

    public ApiServiceCockpitImpl(ObjectMapper objectMapper, ApiService apiService, SwaggerService swaggerService, PageService pageService) {
        this.objectMapper = objectMapper;
        this.apiService = apiService;
        this.swaggerService = swaggerService;
        this.pageService = pageService;
    }

    public ApiEntity createOrUpdateFromCockpit(String apiId, String userId, String swaggerDefinition, String environmentId) {
        GraviteeContext.setCurrentEnvironment(environmentId);

        ImportSwaggerDescriptorEntity swaggerDescriptor = new ImportSwaggerDescriptorEntity();
        swaggerDescriptor.setPayload(swaggerDefinition);
        swaggerDescriptor.setWithDocumentation(true);
        swaggerDescriptor.setWithPolicyPaths(true);
        swaggerDescriptor.setWithPolicies(List.of("mock"));

        final SwaggerApiEntity api = swaggerService.createAPI(swaggerDescriptor, DefinitionVersion.V2);
        api.setPaths(null);

        if (this.apiService.exists(apiId)) {
            return this.apiService.updateFromSwagger(apiId, api, swaggerDescriptor);
        } else {
            return this.createFromCockpit(apiId, userId, swaggerDescriptor, api);
        }
    }

    private ApiEntity createFromCockpit(
        String apiId,
        String userId,
        ImportSwaggerDescriptorEntity swaggerDescriptor,
        SwaggerApiEntity api
    ) {
        final ObjectNode apiDefinition = objectMapper.valueToTree(api);
        apiDefinition.put("id", apiId);

        final ApiEntity createdApi = apiService.createWithApiDefinition(api, userId, apiDefinition);
        pageService.createAsideFolder(apiId, GraviteeContext.getCurrentEnvironment());
        apiService.createOrUpdateDocumentation(swaggerDescriptor, createdApi, true);
        apiService.createMetadata(api.getMetadata(), createdApi.getId());

        return createdApi;
    }
}
