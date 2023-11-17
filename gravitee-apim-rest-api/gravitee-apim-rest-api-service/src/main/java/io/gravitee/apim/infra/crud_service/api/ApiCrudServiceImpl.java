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
package io.gravitee.apim.infra.crud_service.api;

import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api_key.crud_service.ApiKeyCrudService;
import io.gravitee.apim.core.api_key.model.ApiKeyEntity;
import io.gravitee.apim.infra.adapter.ApiAdapter;
import io.gravitee.apim.infra.adapter.ApiKeyAdapter;
import io.gravitee.apim.infra.adapter.PageAdapter;
import io.gravitee.apim.infra.crud_service.documentation.PageCrudServiceImpl;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class ApiCrudServiceImpl implements ApiCrudService {

    private static final Logger logger = LoggerFactory.getLogger(ApiCrudServiceImpl.class);
    private final ApiRepository apiRepository;

    public ApiCrudServiceImpl(@Lazy ApiRepository apiRepository) {
        this.apiRepository = apiRepository;
    }

    @Override
    public Api get(String id) {
        try {
            var foundApi = apiRepository.findById(id);
            if (foundApi.isPresent()) {
                return ApiAdapter.INSTANCE.toEntity(foundApi.get());
            }
        } catch (TechnicalException e) {
            logger.error("An error occurred while finding Api by id {}", id, e);
        }
        throw new ApiNotFoundException(id);
    }
}
