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
package io.gravitee.management.service.impl;

import io.gravitee.management.model.ApiKeyEntity;
import io.gravitee.management.service.ApiKeyGenerator;
import io.gravitee.management.service.ApiKeyService;
import io.gravitee.management.service.exceptions.ApiKeyNotFoundException;
import io.gravitee.management.service.exceptions.TechnicalManagementException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.model.ApiKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Predicate;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
@Component
public class ApiKeyServiceImpl extends TransactionalService implements ApiKeyService {

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

            // Previously generated keys should be set with an expiration date or set as revoked

            // TODO: put rules in configuration
            Instant expirationInst = apiKey.getCreatedAt().toInstant().plus(Duration.ofHours(12));
            Date expirationDate = Date.from(expirationInst);

            Set<ApiKey> oldKeys = apiKeyRepository.findByApplicationAndApi(applicationName, apiName);
            for(ApiKey oldKey : oldKeys) {
                if (! oldKey.isRevoked() && oldKey.getExpiration() == null) {
                    oldKey.setExpiration(expirationDate);
                    apiKeyRepository.update(oldKey);
                }
            }

            apiKey = apiKeyRepository.create(applicationName, apiName, apiKey);
            return convert(apiKey);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to generate a key for {} - {}", applicationName, apiName, ex);
            throw new TechnicalManagementException("An error occurs while trying to generate a key for " + applicationName + " - " + apiName, ex);
        }
    }

    @Override
    public void revoke(String apiKey) {
        try {
            LOGGER.debug("Revoke key {}", apiKey);
            Optional<ApiKey> optKey = apiKeyRepository.retrieve(apiKey);
            if (! optKey.isPresent()) {
                throw new ApiKeyNotFoundException();
            }

            ApiKey key = optKey.get();
            if (! key.isRevoked()) {
                key.setRevoked(true);
                key.setExpiration(new Date());

                apiKeyRepository.update(key);
            }
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to revoke a key {}", apiKey, ex);
            throw new TechnicalManagementException("An error occurs while trying to revoke a key " + apiKey, ex);
        }
    }

    @Override
    public Optional<ApiKeyEntity> current(String applicationName, String apiName) {
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
    public Set<ApiKeyEntity> findAll(String applicationName, String apiName) {
        try {
            LOGGER.debug("Find all API Keys for {} - {}", applicationName, apiName);

            Set<ApiKey> apiKeys = apiKeyRepository.findByApplicationAndApi(applicationName, apiName);
            Set<ApiKeyEntity> apiKeyEntities = new HashSet<>(apiKeys.size());

            for(ApiKey apiKey : apiKeys) {
                apiKeyEntities.add(convert(apiKey));
            }

            return apiKeyEntities;
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find all API Keys for {} - {}", applicationName, apiName);
            throw new TechnicalManagementException("An error occurs while trying to find find all API Keys for " + applicationName + " - " + apiName, ex);
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
