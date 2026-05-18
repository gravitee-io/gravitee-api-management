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
package io.gravitee.apim.infra.api;

import io.gravitee.apim.core.api.service_provider.ApiTemplateModelProvider;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.v4.ApiTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ApiTemplateModelProviderImpl implements ApiTemplateModelProvider {

    private final ApiTemplateService apiTemplateService;

    @Override
    public Object getApiTemplateModel(String organizationId, String environmentId, String apiId) {
        return apiTemplateService.findByIdForTemplates(new ExecutionContext(organizationId, environmentId), apiId, true);
    }
}
