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
import io.gravitee.management.model.*;
import io.gravitee.management.service.*;
import io.gravitee.management.service.builder.EmailNotificationBuilder;
import io.gravitee.management.service.exceptions.ApiKeyNotFoundException;
import io.gravitee.management.service.exceptions.SubscriptionClosedException;
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
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
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
    private SubscriptionService subscriptionService;

    @Autowired
    private ApiKeyGenerator apiKeyGenerator;

    @Autowired
    private EmailService emailService;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private ApiService apiService;

    @Autowired
    private PlanService planService;

    @Override
    public ApiKeyEntity generate(String subscription) {
        try {
            LOGGER.debug("Generate an API Key for subscription {}", subscription);

            ApiKey apiKey = generateForSubscription(subscription);
            apiKey = apiKeyRepository.create(apiKey);

            //TODO: Send a notification to the application owner

            return convert(apiKey);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to generate an API Key for {} - {}", subscription, ex);
            throw new TechnicalManagementException(
                    String.format("An error occurs while trying to generate an API Key for %s", subscription), ex);
        }
    }

    @Override
    public ApiKeyEntity renew(String subscription) {
        try {
            LOGGER.debug("Renew API Key for subscription {}", subscription);

            ApiKey newApiKey = generateForSubscription(subscription);
            newApiKey = apiKeyRepository.create(newApiKey);

            Instant expirationInst = newApiKey.getCreatedAt().toInstant().plus(Duration.ofHours(2));
            Date expirationDate = Date.from(expirationInst);

            // Previously generated keys should be set as revoked
            // Get previously generated keys to set their expiration date
            Set<ApiKey> oldKeys = apiKeyRepository.findBySubscription(subscription);
            for (ApiKey oldKey : oldKeys) {
                if (! oldKey.equals(newApiKey)) {
                    setExpiration(expirationDate, oldKey);
                }
            }

            //TODO: Send a notification to the application owner

            return convert(newApiKey);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to renew an API Key for {}", subscription, ex);
            throw new TechnicalManagementException(
                    String.format("An error occurs while trying to renew an API Key for %s", subscription), ex);
        }
    }

    /**
     * Generate an {@link ApiKey} from a subscription.
     *
     * @param subscription
     * @return An Api Key
     */
    private ApiKey generateForSubscription(String subscription) {
        SubscriptionEntity subscriptionEntity = subscriptionService.findById(subscription);

        Date now = new Date();
        if (subscriptionEntity.getEndingAt() != null && subscriptionEntity.getEndingAt().before(now)) {
            throw new SubscriptionClosedException(subscription);
        }

        ApiKey apiKey = new ApiKey();
        apiKey.setSubscription(subscription);
        apiKey.setApplication(subscriptionEntity.getApplication());
        apiKey.setPlan(subscriptionEntity.getPlan());
        apiKey.setCreatedAt(new Date());
        apiKey.setUpdatedAt(apiKey.getCreatedAt());
        apiKey.setKey(apiKeyGenerator.generate());

        // By default, the API Key will expire when subscription is closed
        apiKey.setExpireAt(subscriptionEntity.getEndingAt());

        return apiKey;
    }

    @Override
    public void revoke(String apiKey, boolean notify) {
        try {
            LOGGER.debug("Revoke API Key {}", apiKey);
            Optional<ApiKey> optKey = apiKeyRepository.findById(apiKey);
            if (!optKey.isPresent()) {
                throw new ApiKeyNotFoundException();
            }

            ApiKey key = optKey.get();
            if (!key.isRevoked()) {
                key.setRevoked(true);
                key.setRevokedAt(new Date());

                apiKeyRepository.update(key);

                if (notify) {
                    final ApplicationEntity application = applicationService.findById(key.getApplication());
                    final PlanEntity plan = planService.findById(key.getPlan());
                    final ApiModelEntity api = apiService.findByIdForTemplates(plan.getApis().iterator().next());
                    final PrimaryOwnerEntity owner = application.getPrimaryOwner();

                    if (owner != null && owner.getEmail() != null && !owner.getEmail().isEmpty()) {
                        emailService.sendAsyncEmailNotification(new EmailNotificationBuilder()
                                .to(owner.getEmail())
                                .subject("API key revoked for API " + api.getName())
                                .template(EmailNotificationBuilder.EmailTemplate.REVOKE_API_KEY)
                                .params(ImmutableMap.of(
                                        "owner", owner,
                                        "api", api,
                                        "plan", plan,
                                        "application", application,
                                        "apiKey", key.getKey()))
                                .build());
                    }
                }
            } else {
                LOGGER.info("API Key {} already revoked. Skipping...", apiKey);
            }
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to revoke a key {}", apiKey, ex);
            throw new TechnicalManagementException("An error occurs while trying to revoke a key " + apiKey, ex);
        }
    }

    @Override
    public Set<ApiKeyEntity> findBySubscription(String subscription) {
        try {
            LOGGER.debug("Find API Keys for subscription {}", subscription);

            SubscriptionEntity subscriptionEntity = subscriptionService.findById(subscription);
            Set<ApiKey> keys = apiKeyRepository.findBySubscription(subscriptionEntity.getId());
            return keys.stream().map(ApiKeyServiceImpl::convert).collect(Collectors.toSet());
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while finding API keys for subscription {}", subscription, ex);
            throw new TechnicalManagementException(
                    String.format("An error occurs while finding API keys for subscription %s", subscription), ex);
        }
    }

    @Override
    public ApiKeyEntity findByKey(String apiKey) {
        try {
            LOGGER.debug("Find an API Key by key: {}", apiKey);

            Optional<ApiKey> optApiKey = apiKeyRepository.findById(apiKey);

            if (optApiKey.isPresent()) {
                return convert(optApiKey.get());
            }

            throw new ApiKeyNotFoundException();
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find an API Key by key {}", apiKey, ex);
            throw new TechnicalManagementException(
                    String.format("An error occurs while trying to find an API Key by key: %s", apiKey), ex);
        }
    }

    @Override
    public ApiKeyEntity update(ApiKeyEntity apiKeyEntity) {
        try {
            LOGGER.debug("Update API Key {}", apiKeyEntity.getKey());
            Optional<ApiKey> optKey = apiKeyRepository.findById(apiKeyEntity.getKey());
            if (!optKey.isPresent()) {
                throw new ApiKeyNotFoundException();
            }

            ApiKey key = optKey.get();

            setExpiration(apiKeyEntity.getExpireAt(), key);

            return convert(key);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while updating an API Key {}", apiKeyEntity.getKey(), ex);
            throw new TechnicalManagementException(String.format(
                    "An error occurs while updating an API Key %s", apiKeyEntity.getKey()), ex);
        }
    }

    @Override
    public void delete(String apiKey) {
        /*
        try {
            LOGGER.debug("Delete API Key {}", apiKey);
            Optional<ApiKey> optKey = apiKeyRepository.de(apiKey);
            if (!optKey.isPresent()) {
                throw new ApiKeyNotFoundException();
            }

            ApiKey key = optKey.get();

            setExpiration(apiKeyEntity.getExpireAt(), key);

            return convert(key);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to update a key {}", apiKey, ex);
            throw new TechnicalManagementException("An error occurs while trying to update a key " + apiKey, ex);
        }
        */
    }

    private void setExpiration(Date expirationDate, ApiKey key) throws TechnicalException {
        if (!key.isRevoked() && key.getExpireAt() == null) {
            key.setExpireAt(expirationDate);
            apiKeyRepository.update(key);

            final ApplicationEntity application = applicationService.findById(key.getApplication());
            final PlanEntity plan = planService.findById(key.getPlan());
            final ApiModelEntity api = apiService.findByIdForTemplates(plan.getApis().iterator().next());
            final PrimaryOwnerEntity owner = application.getPrimaryOwner();

            if (owner != null && owner.getEmail() != null && !owner.getEmail().isEmpty()) {
                emailService.sendAsyncEmailNotification(new EmailNotificationBuilder()
                        .to(owner.getEmail())
                        .subject("API key has been updated !")
                        .template(EmailNotificationBuilder.EmailTemplate.EXPIRE_API_KEY)
                        .params(ImmutableMap.of(
                                "owner", owner,
                                "api", api,
                                "application", application,
                                "apiKey", key.getKey(),
                                "plan", plan))
                        .build());
            }
        }
    }

    private static ApiKeyEntity convert(ApiKey apiKey) {
        ApiKeyEntity apiKeyEntity = new ApiKeyEntity();

        apiKeyEntity.setKey(apiKey.getKey());
        apiKeyEntity.setCreatedAt(apiKey.getCreatedAt());
        apiKeyEntity.setExpireAt(apiKey.getExpireAt());
        apiKeyEntity.setRevoked(apiKey.isRevoked());
        apiKeyEntity.setRevokedAt(apiKey.getRevokedAt());
        apiKeyEntity.setUpdatedAt(apiKey.getUpdatedAt());
        apiKeyEntity.setSubscription(apiKey.getSubscription());
        apiKeyEntity.setApplication(apiKey.getApplication());
        apiKeyEntity.setPlan(apiKey.getPlan());

        return apiKeyEntity;
    }
}
