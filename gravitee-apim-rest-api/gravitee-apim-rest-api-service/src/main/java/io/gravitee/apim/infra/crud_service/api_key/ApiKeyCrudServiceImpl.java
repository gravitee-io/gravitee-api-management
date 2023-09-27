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
package io.gravitee.apim.infra.crud_service.api_key;

import io.gravitee.apim.core.api_key.crud_service.ApiKeyCrudService;
import io.gravitee.apim.core.api_key.model.ApiKeyEntity;
import io.gravitee.apim.infra.adapter.ApiKeyAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class ApiKeyCrudServiceImpl implements ApiKeyCrudService {

    private final ApiKeyRepository apiKeyRepository;

    public ApiKeyCrudServiceImpl(@Lazy ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    @Override
    public ApiKeyEntity update(ApiKeyEntity apiKey) {
        try {
            var result = apiKeyRepository.update(ApiKeyAdapter.INSTANCE.fromEntity(apiKey));
            return ApiKeyAdapter.INSTANCE.toEntity(result);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException("An error occurs while trying to update the api key: " + apiKey.getId(), e);
        }
    }
}
