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
package io.gravitee.management.api.service.impl;

import io.gravitee.management.api.exceptions.TechnicalManagementException;
import io.gravitee.management.api.model.ApiKeyEntity;
import io.gravitee.management.api.service.ApiKeyGenerator;
import io.gravitee.management.api.service.ApiKeyService;
import io.gravitee.repository.api.ApiKeyRepository;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.model.ApiKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Date;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
@Component
public class ApiKeyServiceImpl implements ApiKeyService {

    /**
     * Logger.
     */
    private final Logger LOGGER = LoggerFactory.getLogger(ApiKeyServiceImpl.class);

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private ApiKeyGenerator apiKeyGenerator;

    @Override
    public ApiKeyEntity generate(String applicationName, String apiName) {
        try {
            LOGGER.debug("Generate a new key for {} - {}", applicationName, apiName);
            ApiKey apiKey = new ApiKey();

            apiKey.setCreatedAt(new Date());
            apiKey.setKey(apiKeyGenerator.generate());
            apiKey.setRevoked(false);

            apiKey = apiKeyRepository.create(applicationName, apiName, apiKey);

            // TODO: Previously generated keys should be set with an expiration date or set as revoked
            return convert(apiKey);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to generate a key for {} - {}", applicationName, apiName, ex);
            throw new TechnicalManagementException("An error occurs while trying to generate a key for " + applicationName + " - " + apiName, ex);
        }
    }

    @Override
    public Optional<ApiKeyEntity> getCurrentApiKey(String applicationName, String apiName) {
        try {
            LOGGER.debug("Generate a new key for {} - {}", applicationName, apiName);
            // Current Api Key is the key with the latest createdAt value and which is not revoked
            return apiKeyRepository.findByApplicationAndApi(applicationName, apiName).stream().filter(new Predicate<ApiKey>() {
                @Override
                public boolean test(ApiKey apiKey) {
                    return ! apiKey.isRevoked();
                }
            }).max(new Comparator<ApiKey>() {
                @Override
                public int compare(ApiKey o1, ApiKey o2) {
                    return o1.getCreatedAt().compareTo(o2.getCreatedAt());
                }
            }).map(ApiKeyServiceImpl::convert);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while getting current API key for {} - {}", applicationName, apiName, ex);
            throw new TechnicalManagementException("An error occurs while getting current API key for " + applicationName + " - " + apiName, ex);
        }
    }

    @Override
    public Optional<ApiKeyEntity> getApiKey(String apiKey) {
        try {
            LOGGER.debug("Get API Key using key: {}", apiKey);

            return apiKeyRepository.retrieve(apiKey).map(ApiKeyServiceImpl::convert);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find an API Key using its key {}", apiKey, ex);
            throw new TechnicalManagementException("An error occurs while trying to find an API Key using its key " + apiKey, ex);
        }
    }

    private static ApiKeyEntity convert(ApiKey apiKey) {
        ApiKeyEntity apiKeyEntity = new ApiKeyEntity();

        apiKeyEntity.setKey(apiKey.getKey());
        apiKeyEntity.setCreatedAt(apiKey.getCreatedAt());
        apiKeyEntity.setExpireOn(apiKey.getExpiration());
        apiKeyEntity.setRevoked(apiKey.isRevoked());

        return apiKeyEntity;
    }
}
