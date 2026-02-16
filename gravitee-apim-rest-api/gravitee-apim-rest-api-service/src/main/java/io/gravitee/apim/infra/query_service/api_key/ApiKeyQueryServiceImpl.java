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
package io.gravitee.apim.infra.query_service.api_key;

import io.gravitee.apim.core.api_key.model.ApiKeyEntity;
import io.gravitee.apim.core.api_key.query_service.ApiKeyQueryService;
import io.gravitee.apim.infra.adapter.ApiKeyAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.Optional;
import java.util.stream.Stream;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class ApiKeyQueryServiceImpl implements ApiKeyQueryService {

    private final ApiKeyRepository apiKeyRepository;

    public ApiKeyQueryServiceImpl(@Lazy ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    @Override
    public Optional<ApiKeyEntity> findById(String id) {
        try {
            return apiKeyRepository.findById(id).map(ApiKeyAdapter.INSTANCE::toEntity);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException("An error occurs while trying to find API key by id: " + id, e);
        }
    }

    @Override
    public Stream<ApiKeyEntity> findByApplication(String applicationId) {
        try {
            return apiKeyRepository.findByApplication(applicationId).stream().map(ApiKeyAdapter.INSTANCE::toEntity);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException("An error occurs while trying to find API keys by application id: " + applicationId, e);
        }
    }

    @Override
    public Optional<ApiKeyEntity> findByKeyAndApiId(String key, String apiId) {
        try {
            return apiKeyRepository.findByKeyAndApi(key, apiId).map(ApiKeyAdapter.INSTANCE::toEntity);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException(
                String.format("An error occurs while trying to find API key by [key=%s] and [apiId=%s]", key, apiId),
                e
            );
        }
    }

    @Override
    public Stream<ApiKeyEntity> findBySubscription(String subscriptionId) {
        try {
            return apiKeyRepository.findBySubscription(subscriptionId).stream().map(ApiKeyAdapter.INSTANCE::toEntity);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException(
                "An error occurs while trying to find API keys by subscription id: " + subscriptionId,
                e
            );
        }
    }

    @Override
    public Optional<ApiKeyEntity> findByKeyAndReferenceIdAndReferenceType(String key, String referenceId, String referenceType) {
        try {
            if (!"API".equals(referenceType) && !"API_PRODUCT".equals(referenceType)) {
                throw new IllegalArgumentException("Unsupported reference type: " + referenceType);
            }
            return apiKeyRepository
                .findByKeyAndReferenceIdAndReferenceType(key, referenceId, referenceType)
                .map(ApiKeyAdapter.INSTANCE::toEntity);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException(
                String.format(
                    "An error occurs while trying to find API key by [key=%s], [referenceId=%s], [referenceType=%s]",
                    key,
                    referenceId,
                    referenceType
                ),
                e
            );
        }
    }
}
