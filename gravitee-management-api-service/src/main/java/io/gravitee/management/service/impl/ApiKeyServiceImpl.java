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

import com.google.common.collect.ImmutableMap;
import io.gravitee.management.model.ApiKeyEntity;
import io.gravitee.management.model.ApplicationEntity;
import io.gravitee.management.model.PrimaryOwnerEntity;
import io.gravitee.management.service.*;
import io.gravitee.management.service.builder.EmailNotificationBuilder;
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
import java.util.stream.Collectors;

import static java.util.Collections.emptySet;

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

    @Autowired
    private EmailService emailService;

    @Autowired
    private ApplicationService applicationService;

    @Override
    public ApiKeyEntity generateOrRenew(String applicationName, String apiName) {
        try {
            LOGGER.debug("Generate a new key for {} - {}", applicationName, apiName);
            ApiKey apiKey = new ApiKey();

            apiKey.setApplication(applicationName);
            apiKey.setApi(apiName);
            apiKey.setCreatedAt(new Date());
            apiKey.setKey(apiKeyGenerator.generate());
            apiKey.setRevoked(false);

            Instant expirationInst = apiKey.getCreatedAt().toInstant().plus(Duration.ofHours(1));
            Date expirationDate = Date.from(expirationInst);

            // Previously generated keys should be set as revoked
            Set<ApiKey> oldKeys = apiKeyRepository.findByApplicationAndApi(applicationName, apiName);
            for (ApiKey oldKey : oldKeys) {
                setExpiration(expirationDate, oldKey);
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
            if (!optKey.isPresent()) {
                throw new ApiKeyNotFoundException();
            }

            ApiKey key = optKey.get();
            if (!key.isRevoked()) {
                key.setRevoked(true);
                key.setRevokeAt(new Date());

                apiKeyRepository.update(key);

                final ApplicationEntity applicationEntity = applicationService.findById(key.getApplication());
                final PrimaryOwnerEntity owner = applicationEntity.getPrimaryOwner();

                if (owner != null && owner.getEmail() != null && !owner.getEmail().isEmpty()) {
                    emailService.sendAsyncEmailNotification(new EmailNotificationBuilder()
                            .to(owner.getEmail())
                            .subject("An API key has been revoked on API " + key.getApi() + " for application " + key.getApplication())
                            .content("apiKeyRevoked.html")
                            .params(ImmutableMap.of( "owner", owner.getUsername(), "api", key.getApi(),
                                    "application", key.getApplication(), "apiKey", key.getKey()))
                            .build());
                }
            }
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to revoke a key {}", apiKey, ex);
            throw new TechnicalManagementException("An error occurs while trying to revoke a key " + apiKey, ex);
        }
    }

    @Override
    public Optional<ApiKeyEntity> getCurrent(String applicationName, String apiName) {
        try {
            LOGGER.debug("Generate a new key for {} - {}", applicationName, apiName);
            // Current Api Key is the key with the latest createdAt value and which is not revoked
            final Set<ApiKey> apiKeys = apiKeyRepository.findByApplicationAndApi(applicationName, apiName);
            if (apiKeys == null || apiKeys.isEmpty()) {
                return Optional.empty();
            }
            return apiKeys.stream()
                .filter(apiKey -> !apiKey.isRevoked())
                .max((o1, o2) -> o1.getCreatedAt().compareTo(o2.getCreatedAt()))
                .map(ApiKeyServiceImpl::convert);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while getting current API key for {} - {}", applicationName, apiName, ex);
            throw new TechnicalManagementException("An error occurs while getting current API key for " + applicationName + " - " + apiName, ex);
        }
    }

    @Override
    public Map<String, List<ApiKeyEntity>> findByApplication(String applicationId) {
        try {
            LOGGER.debug("Find all API keys for application {}", applicationId);

            Set<ApiKey> keys = apiKeyRepository.findByApplication(applicationId);

            Map<String, Set<ApiKey>> keysByApi = new HashMap<>();
            keys.forEach(apiKey -> {
                Set<ApiKey> values = keysByApi.getOrDefault(apiKey.getApi(), new HashSet<>());
                values.add(apiKey);
                keysByApi.put(apiKey.getApi(), values);
            });

            Map<String, List<ApiKeyEntity>> keysByApiResult = new HashMap<>(keysByApi.size());

            keysByApi.forEach((api, apiKeys) -> keysByApiResult.put(api,
                apiKeys.stream().sorted(
                        (key1, key2) -> key2.getCreatedAt().compareTo(
                                key1.getCreatedAt())).map(ApiKeyServiceImpl::convert).collect(Collectors.toList())));

            return keysByApiResult;
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while getting all API keys for application {}", applicationId, ex);
            throw new TechnicalManagementException("An error occurs while getting all API keys for application " + applicationId, ex);
        }
    }

    @Override
    public Map<String, List<ApiKeyEntity>> findByApi(String apiId) {
        try {
            LOGGER.debug("Find all API keys for API {}", apiId);

            Set<ApiKey> keys = apiKeyRepository.findByApi(apiId);

            Map<String, Set<ApiKey>> keysByApplication = new HashMap<>();
            keys.forEach(apiKey -> {
                Set<ApiKey> values = keysByApplication.getOrDefault(apiKey.getApplication(), new HashSet<>());
                values.add(apiKey);
                keysByApplication.put(apiKey.getApplication(), values);
            });

            Map<String, List<ApiKeyEntity>> keysByApplicationResult = new HashMap<>(keysByApplication.size());

            keysByApplication.forEach((api, apiKeys) -> keysByApplicationResult.put(api,
                    apiKeys.stream().sorted(
                            (key1, key2) -> key2.getCreatedAt().compareTo(
                                    key1.getCreatedAt())).map(ApiKeyServiceImpl::convert).collect(Collectors.toList())));

            return keysByApplicationResult;
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while getting all API keys for API {}", apiId, ex);
            throw new TechnicalManagementException("An error occurs while getting all API keys for API " + apiId, ex);
        }
    }

    @Override
    public ApiKeyEntity update(String apiKey, ApiKeyEntity apiKeyEntity) {
        try {
            LOGGER.debug("Trying to update key {}", apiKey);
            Optional<ApiKey> optKey = apiKeyRepository.retrieve(apiKey);
            if (!optKey.isPresent()) {
                throw new ApiKeyNotFoundException();
            }

            ApiKey key = optKey.get();

            setExpiration(apiKeyEntity.getExpireOn(), key);

            return convert(key);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to update a key {}", apiKey, ex);
            throw new TechnicalManagementException("An error occurs while trying to update a key " + apiKey, ex);
        }
    }

    private void setExpiration(Date expirationDate, ApiKey key) throws TechnicalException {
        if (!key.isRevoked() && key.getExpiration() == null) {
            key.setExpiration(expirationDate);
            apiKeyRepository.update(key);

            final ApplicationEntity applicationEntity = applicationService.findById(key.getApplication());
            final PrimaryOwnerEntity owner = applicationEntity.getPrimaryOwner();

            if (owner != null && owner.getEmail() != null && !owner.getEmail().isEmpty()) {
                emailService.sendAsyncEmailNotification(new EmailNotificationBuilder()
                        .to(owner.getEmail())
                        .subject("An API key has been revoked on API " + key.getApi() + " for application " + key.getApplication())
                        .content("apiKeyRevoked.html")
                        .params(ImmutableMap.of( "owner", owner.getUsername(), "api", key.getApi(),
                                "application", key.getApplication(), "apiKey", key.getKey()))
                        .build());
            }
        }
    }

    @Override
    public Set<ApiKeyEntity> findAll(String applicationName, String apiName) {
        try {
            LOGGER.debug("Find all API Keys for {} - {}", applicationName, apiName);

            final Set<ApiKey> apiKeys = apiKeyRepository.findByApplicationAndApi(applicationName, apiName);

            if (apiKeys == null || apiKeys.isEmpty()) {
                return emptySet();
            }

            final Set<ApiKeyEntity> apiKeyEntities = new HashSet<>(apiKeys.size());

            apiKeyEntities.addAll(apiKeys.stream()
                    .map(ApiKeyServiceImpl::convert)
                    .collect(Collectors.toSet())
            );

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
        apiKeyEntity.setRevokeAt(apiKey.getRevokeAt());
        apiKeyEntity.setApi(apiKey.getApi());
        apiKeyEntity.setApplication(apiKey.getApplication());

        return apiKeyEntity;
    }
}
