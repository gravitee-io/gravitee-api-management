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

import static io.gravitee.repository.management.model.ApiKey.AuditEvent.*;
import static io.gravitee.repository.management.model.Audit.AuditProperties.*;
import static java.util.Comparator.*;
import static java.util.Comparator.comparing;
import static java.util.Comparator.reverseOrder;
import static java.util.stream.Collectors.*;
import static org.apache.commons.lang3.StringUtils.*;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.api.search.ApiKeyCriteria;
import io.gravitee.repository.management.model.ApiKey;
import io.gravitee.repository.management.model.Audit;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.key.ApiKeyQuery;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.exceptions.*;
import io.gravitee.rest.api.service.notification.ApiHook;
import io.gravitee.rest.api.service.notification.NotificationParamsBuilder;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
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
    private ApplicationService applicationService;

    @Autowired
    private ApiService apiService;

    @Autowired
    private PlanService planService;

    @Autowired
    private AuditService auditService;

    @Autowired
    private NotifierService notifierService;

    @Override
    public ApiKeyEntity generate(
        ExecutionContext executionContext,
        ApplicationEntity application,
        SubscriptionEntity subscription,
        String customApiKey
    ) {
        if (!application.hasApiKeySharedMode()) {
            return generate(executionContext, subscription, customApiKey);
        }
        return findOrGenerate(executionContext, application, subscription, customApiKey);
    }

    @Override
    public ApiKeyEntity renew(ExecutionContext executionContext, SubscriptionEntity subscription) {
        return renew(executionContext, subscription, null);
    }

    @Override
    public ApiKeyEntity renew(ExecutionContext executionContext, SubscriptionEntity subscription, String customApiKey) {
        try {
            LOGGER.debug("Renew API Key for subscription {}", subscription.getId());

            ApiKey newApiKey = generateForSubscription(executionContext, subscription, customApiKey);
            newApiKey = apiKeyRepository.create(newApiKey);

            // Expire previously generated keys
            expireApiKeys(executionContext, apiKeyRepository.findBySubscription(subscription.getId()), newApiKey);

            ApiKeyEntity newApiKeyEntity = convert(executionContext, newApiKey);
            // Audit
            createAuditLog(executionContext, newApiKeyEntity, null, APIKEY_RENEWED, newApiKey.getCreatedAt());
            // Notification
            triggerNotifierService(
                executionContext,
                ApiHook.APIKEY_RENEWED,
                newApiKey,
                newApiKeyEntity.getApplication(),
                newApiKeyEntity.getSubscriptions()
            );

            return newApiKeyEntity;
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to renew an API Key for {}", subscription.getId(), ex);
            throw new TechnicalManagementException(
                String.format("An error occurs while trying to renew an API Key for %s", subscription.getId()),
                ex
            );
        }
    }

    @Override
    public ApiKeyEntity renew(ExecutionContext executionContext, ApplicationEntity application) {
        if (!application.hasApiKeySharedMode()) {
            throw new InvalidApplicationApiKeyModeException("Can't renew an API key on application that doesn't use shared API key mode");
        }

        try {
            LOGGER.debug("Renew API Key for application {}", application.getId());

            ApiKey newApiKey = generateForApplication(application.getId());
            newApiKey = apiKeyRepository.create(newApiKey);

            // Expire previously generated keys
            Collection<ApiKey> allApiKeys = apiKeyRepository.findByApplication(application.getId());
            expireApiKeys(executionContext, allApiKeys, newApiKey);

            // add all subscriptions to the new key
            addSharedSubscriptions(allApiKeys, newApiKey);

            ApiKeyEntity newApiKeyEntity = convert(executionContext, newApiKey);

            createAuditLog(executionContext, newApiKeyEntity, null, APIKEY_RENEWED, newApiKey.getCreatedAt());

            triggerNotifierService(
                executionContext,
                ApiHook.APIKEY_RENEWED,
                newApiKey,
                newApiKeyEntity.getApplication(),
                newApiKeyEntity.getSubscriptions()
            );

            return newApiKeyEntity;
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to renew an API Key for application {}", application.getId(), ex);
            throw new TechnicalManagementException(
                String.format("An error occurs while trying to renew an API Key for application %s", application.getId()),
                ex
            );
        }
    }

    private void expireApiKeys(ExecutionContext executionContext, Collection<ApiKey> apiKeys, ApiKey activeApiKey)
        throws TechnicalException {
        Instant expirationInst = activeApiKey.getCreatedAt().toInstant().plus(Duration.ofHours(2));
        Date expirationDate = Date.from(expirationInst);

        for (ApiKey apiKey : apiKeys) {
            ApiKeyEntity apiKeyEntity = convert(executionContext, apiKey);
            if (!apiKey.equals(activeApiKey) && !apiKeyEntity.isExpired()) {
                setExpiration(executionContext, expirationDate, apiKeyEntity, apiKey);
            }
        }
    }

    private void addSharedSubscriptions(Collection<ApiKey> apiKeys, ApiKey activeApiKey) throws TechnicalException {
        Set<String> subscriptions = new HashSet<>();
        apiKeys.forEach(apiKey -> subscriptions.addAll(apiKey.getSubscriptions()));
        activeApiKey.setSubscriptions(new ArrayList<>(subscriptions));
        activeApiKey.setUpdatedAt(new Date());
        apiKeyRepository.update(activeApiKey);
    }

    private ApiKeyEntity findOrGenerate(
        ExecutionContext executionContext,
        ApplicationEntity application,
        SubscriptionEntity subscription,
        String customApiKey
    ) {
        return findByApplication(executionContext, application.getId())
            .stream()
            .peek(apiKey -> addSubscription(apiKey, subscription))
            .max(comparing(ApiKeyEntity::isRevoked, reverseOrder()).thenComparing(ApiKeyEntity::getExpireAt, nullsLast(naturalOrder())))
            .orElseGet(() -> generate(executionContext, subscription, customApiKey));
    }

    private void addSubscription(ApiKeyEntity apiKeyEntity, SubscriptionEntity subscription) {
        try {
            ApiKey apiKey = apiKeyRepository.findById(apiKeyEntity.getId()).orElseThrow(ApiKeyNotFoundException::new);
            ArrayList<String> subscriptions = new ArrayList<>(apiKey.getSubscriptions());
            subscriptions.add(subscription.getId());
            apiKey.setSubscriptions(subscriptions);
            apiKey.setUpdatedAt(new Date());
            apiKeyRepository.update(apiKey);
        } catch (TechnicalException e) {
            LOGGER.error("An error occurred while trying to add subscription to API Key", e);
            throw new TechnicalManagementException("An error occurred while trying to a add subscription to API Key");
        }
    }

    private ApiKeyEntity generate(ExecutionContext executionContext, SubscriptionEntity subscription, String customApiKey) {
        try {
            LOGGER.debug("Generate an API Key for subscription {}", subscription);

            ApiKey apiKey = generateForSubscription(executionContext, subscription, customApiKey);
            apiKey = apiKeyRepository.create(apiKey);

            //TODO: Send a notification to the application owner

            ApiKeyEntity apiKeyEntity = convert(executionContext, apiKey);
            // Audit
            createAuditLog(executionContext, apiKeyEntity, null, APIKEY_CREATED, apiKey.getCreatedAt());

            return apiKeyEntity;
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to generate an API Key for {}", subscription, ex);
            throw new TechnicalManagementException(
                String.format("An error occurs while trying to generate an API Key for %s", subscription),
                ex
            );
        }
    }

    /**
     * Generate an {@link ApiKey} from a subscription. If no custom API key, then generate a new one.
     *
     * @param executionContext
     * @param subscription
     * @param customApiKey
     * @return An Api Key
     */
    private ApiKey generateForSubscription(ExecutionContext executionContext, SubscriptionEntity subscription, String customApiKey) {
        if (isNotEmpty(customApiKey) && !canCreate(executionContext, customApiKey, subscription)) {
            throw new ApiKeyAlreadyExistingException();
        }

        Date now = new Date();
        if (subscription.getEndingAt() != null && subscription.getEndingAt().before(now)) {
            throw new SubscriptionClosedException(subscription.getId());
        }

        ApiKey apiKey = generateForApplication(subscription.getApplication(), customApiKey);
        apiKey.setSubscriptions(List.of(subscription.getId()));

        // By default, the API Key will expire when subscription is closed
        apiKey.setExpireAt(subscription.getEndingAt());

        return apiKey;
    }

    /**
     * Generate an {@link ApiKey} for an application. Generates a new random key value.
     *
     * @param application
     * @return An Api Key
     */
    private ApiKey generateForApplication(String application) {
        return generateForApplication(application, null);
    }

    /**
     * Generate an {@link ApiKey} for an application. If no custom API key, then generate a new one.
     *
     * @param application
     * @param customApiKey
     * @return An Api Key
     */
    private ApiKey generateForApplication(String application, String customApiKey) {
        ApiKey apiKey = new ApiKey();
        apiKey.setId(UuidString.generateRandom());
        apiKey.setApplication(application);
        apiKey.setCreatedAt(new Date());
        apiKey.setUpdatedAt(apiKey.getCreatedAt());
        apiKey.setKey(isNotEmpty(customApiKey) ? customApiKey : apiKeyGenerator.generate());
        return apiKey;
    }

    @Override
    public void revoke(ExecutionContext executionContext, String keyId, boolean notify) {
        try {
            ApiKey key = apiKeyRepository.findById(keyId).orElseThrow(ApiKeyNotFoundException::new);
            revoke(executionContext, key, notify);
        } catch (TechnicalException e) {
            String message = String.format("An error occurs while trying to revoke a key with id %s", keyId);
            LOGGER.error(message, e);
            throw new TechnicalManagementException(message, e);
        }
    }

    @Override
    public void revoke(ExecutionContext executionContext, ApiKeyEntity apiKeyEntity, boolean notify) {
        revoke(executionContext, apiKeyEntity.getId(), notify);
    }

    private void revoke(ExecutionContext executionContext, ApiKey key, boolean notify) throws TechnicalException {
        LOGGER.debug("Revoke API Key with id {}", key.getId());

        checkApiKeyExpired(executionContext, key);

        ApiKey previousApiKey = new ApiKey(key);
        key.setRevoked(true);
        key.setUpdatedAt(new Date());
        key.setRevokedAt(key.getUpdatedAt());

        apiKeyRepository.update(key);

        // Audit
        createAuditLog(executionContext, convert(executionContext, previousApiKey), previousApiKey, APIKEY_REVOKED, key.getUpdatedAt());

        // notify
        if (notify) {
            triggerNotifierService(executionContext, ApiHook.APIKEY_REVOKED, key, new NotificationParamsBuilder());
        }
    }

    @Override
    public ApiKeyEntity reactivate(ExecutionContext executionContext, ApiKeyEntity apiKeyEntity) {
        try {
            ApiKey key = apiKeyRepository.findById(apiKeyEntity.getId()).orElseThrow(ApiKeyNotFoundException::new);

            LOGGER.debug("Reactivate API Key id {}", apiKeyEntity.getId());

            if (!key.isRevoked() && !convert(executionContext, key).isExpired()) {
                throw new ApiKeyAlreadyActivatedException();
            }

            ApiKey previousApiKey = new ApiKey(key);
            key.setRevoked(false);
            key.setUpdatedAt(new Date());
            key.setRevokedAt(null);

            // If this is not a shared API key,
            // Get the subscription to get ending date and set key expiration date
            if (!apiKeyEntity.getApplication().hasApiKeySharedMode()) {
                SubscriptionEntity subscription = subscriptionService.findById(key.getSubscriptions().get(0));
                if (subscription.getStatus() != SubscriptionStatus.PAUSED && subscription.getStatus() != SubscriptionStatus.ACCEPTED) {
                    throw new SubscriptionNotActiveException(subscription);
                }
                key.setExpireAt(subscription.getEndingAt());
            }

            ApiKey updated = apiKeyRepository.update(key);
            ApiKeyEntity updatedEntity = convert(executionContext, updated);
            // Audit
            createAuditLog(executionContext, updatedEntity, previousApiKey, APIKEY_REACTIVATED, key.getUpdatedAt());

            return updatedEntity;
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to reactivate an api key", ex);
            throw new TechnicalManagementException("An error occurs while trying to reactivate an api key", ex);
        }
    }

    private void checkApiKeyExpired(ExecutionContext executionContext, ApiKey key) {
        if (key.isRevoked() || convert(executionContext, key).isExpired()) {
            throw new ApiKeyAlreadyExpiredException();
        }
    }

    @Override
    public ApiKeyEntity findById(ExecutionContext executionContext, String keyId) {
        try {
            return apiKeyRepository
                .findById(keyId)
                .map(apiKey -> convert(executionContext, apiKey))
                .orElseThrow(ApiKeyNotFoundException::new);
        } catch (TechnicalException e) {
            String message = String.format("An error occurs while trying to find a key with id %s", keyId);
            LOGGER.error(message, e);
            throw new TechnicalManagementException(message, e);
        }
    }

    @Override
    public List<ApiKeyEntity> findByKey(ExecutionContext executionContext, String apiKey) {
        try {
            LOGGER.debug("Find API Keys by key");
            return apiKeyRepository.findByKey(apiKey).stream().map(apiKey1 -> convert(executionContext, apiKey1)).collect(toList());
        } catch (TechnicalException e) {
            LOGGER.error("An error occurs while finding API keys", e);
            throw new TechnicalManagementException("An error occurs while finding API keys", e);
        }
    }

    @Override
    public List<ApiKeyEntity> findBySubscription(ExecutionContext executionContext, String subscription) {
        try {
            LOGGER.debug("Find API Keys for subscription {}", subscription);

            SubscriptionEntity subscriptionEntity = subscriptionService.findById(subscription);
            Set<ApiKey> keys = apiKeyRepository.findBySubscription(subscriptionEntity.getId());
            return keys
                .stream()
                .map(apiKey -> convert(executionContext, apiKey))
                .sorted((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()))
                .collect(toList());
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while finding API keys for subscription {}", subscription, ex);
            throw new TechnicalManagementException(
                String.format("An error occurs while finding API keys for subscription %s", subscription),
                ex
            );
        }
    }

    @Override
    public ApiKeyEntity findByKeyAndApi(ExecutionContext executionContext, String apiKey, String apiId) {
        try {
            LOGGER.debug("Find an API Key by key for API {}", apiId);
            ApiKey key = apiKeyRepository.findByKeyAndApi(apiKey, apiId).orElseThrow(ApiKeyNotFoundException::new);
            return convert(executionContext, key);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find an API Key for API {}", apiId, ex);
            throw new TechnicalManagementException(String.format("An error occurs while trying to find an API Key for API %s", apiId), ex);
        }
    }

    @Override
    public List<ApiKeyEntity> findByApplication(ExecutionContext executionContext, String applicationId) {
        try {
            return apiKeyRepository
                .findByApplication(applicationId)
                .stream()
                .map(apiKey -> convert(executionContext, apiKey))
                .collect(toList());
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find API Keys for application {}", applicationId, ex);
            throw new TechnicalManagementException(
                String.format("An error occurs while trying to find API Keys for application %s", applicationId),
                ex
            );
        }
    }

    @Override
    public ApiKeyEntity update(ExecutionContext executionContext, ApiKeyEntity apiKeyEntity) {
        try {
            LOGGER.debug("Update API Key with id {}", apiKeyEntity.getId());
            ApiKey key = apiKeyRepository.findById(apiKeyEntity.getId()).orElseThrow(ApiKeyNotFoundException::new);

            checkApiKeyExpired(executionContext, key);

            key.setSubscriptions(apiKeyEntity.getSubscriptionIds());
            key.setPaused(apiKeyEntity.isPaused());
            if (apiKeyEntity.getExpireAt() != null) {
                setExpiration(executionContext, apiKeyEntity.getExpireAt(), apiKeyEntity, key);
            } else {
                key.setUpdatedAt(new Date());
                apiKeyRepository.update(key);
            }

            return convert(executionContext, key);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while updating an API Key with id {}", apiKeyEntity.getId(), ex);
            throw new TechnicalManagementException(
                String.format("An error occurs while updating an API Key with id %s", apiKeyEntity.getId()),
                ex
            );
        }
    }

    @Override
    public ApiKeyEntity updateDaysToExpirationOnLastNotification(
        ExecutionContext executionContext,
        ApiKeyEntity apiKeyEntity,
        Integer value
    ) {
        try {
            return apiKeyRepository
                .findById(apiKeyEntity.getId())
                .map(
                    dbApiKey -> {
                        dbApiKey.setDaysToExpirationOnLastNotification(value);
                        try {
                            return apiKeyRepository.update(dbApiKey);
                        } catch (TechnicalException ex) {
                            LOGGER.error("An error occurs while trying to update ApiKey with id {}", dbApiKey.getId(), ex);
                            throw new TechnicalManagementException(
                                String.format("An error occurs while trying to update ApiKey with id %s", dbApiKey.getId()),
                                ex
                            );
                        }
                    }
                )
                .map(apiKey -> convert(executionContext, apiKey))
                .orElseThrow(ApiKeyNotFoundException::new);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to update apiKey", ex);
            throw new TechnicalManagementException("An error occurs while trying to update apiKey", ex);
        }
    }

    @Override
    public boolean canCreate(ExecutionContext executionContext, String apiKeyValue, SubscriptionEntity subscription) {
        return canCreate(executionContext, apiKeyValue, subscription.getApi(), subscription.getApplication());
    }

    @Override
    public boolean canCreate(ExecutionContext executionContext, String apiKey, String apiId, String applicationId) {
        LOGGER.debug("Check if an API Key can be created for api {} and application {}", apiId, applicationId);

        return findByKey(executionContext, apiKey).stream().noneMatch(existingKey -> isConflictingKey(existingKey, apiId, applicationId));
    }

    private boolean isConflictingKey(ApiKeyEntity existingKey, String apiId, String applicationId) {
        if (!existingKey.getApplication().getId().equals(applicationId)) {
            return true;
        }
        return existingKey.getSubscriptions().stream().map(SubscriptionEntity::getApi).anyMatch(apiId::equals);
    }

    @Override
    public Collection<ApiKeyEntity> search(ExecutionContext executionContext, ApiKeyQuery query) {
        try {
            LOGGER.debug("Search api keys {}", query);

            ApiKeyCriteria.Builder builder = toApiKeyCriteriaBuilder(query);

            return apiKeyRepository
                .findByCriteria(builder.build())
                .stream()
                .map(apiKey -> convert(executionContext, apiKey))
                .collect(toList());
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to search api keys: {}", query, ex);
            throw new TechnicalManagementException(String.format("An error occurs while trying to search api keys: {}", query), ex);
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

    private void setExpiration(ExecutionContext executionContext, Date expirationDate, ApiKeyEntity apiKeyEntity, ApiKey key)
        throws TechnicalException {
        final Date now = new Date();

        if (now.after(expirationDate)) {
            expirationDate = now;
        }

        key.setUpdatedAt(now);

        if (!key.isRevoked()) {
            // If API key is not shared
            // The expired date must be <= than the subscription end date
            if (
                apiKeyEntity.getApplication() != null &&
                !apiKeyEntity.getApplication().hasApiKeySharedMode() &&
                !key.getSubscriptions().isEmpty()
            ) {
                SubscriptionEntity subscription = subscriptionService.findById(key.getSubscriptions().get(0));
                if (
                    subscription.getEndingAt() != null &&
                    (expirationDate == null || subscription.getEndingAt().compareTo(expirationDate) < 0)
                ) {
                    expirationDate = subscription.getEndingAt();
                }
            }

            ApiKey oldkey = new ApiKey(key);
            key.setExpireAt(expirationDate);
            key.setDaysToExpirationOnLastNotification(null);
            apiKeyRepository.update(key);

            //notify
            NotificationParamsBuilder paramsBuilder = new NotificationParamsBuilder();
            if (key.getExpireAt() != null && now.before(key.getExpireAt())) {
                paramsBuilder.expirationDate(key.getExpireAt());
            }
            triggerNotifierService(executionContext, ApiHook.APIKEY_EXPIRED, key, paramsBuilder);

            // Audit
            createAuditLog(executionContext, convert(executionContext, key), oldkey, APIKEY_EXPIRED, key.getUpdatedAt());
        } else {
            apiKeyRepository.update(key);
        }
    }

    private ApiKeyEntity convert(ExecutionContext executionContext, ApiKey apiKey) {
        ApiKeyEntity apiKeyEntity = new ApiKeyEntity();

        apiKeyEntity.setId(apiKey.getId());
        apiKeyEntity.setKey(apiKey.getKey());
        apiKeyEntity.setCreatedAt(apiKey.getCreatedAt());
        apiKeyEntity.setExpireAt(apiKey.getExpireAt());
        apiKeyEntity.setExpired(apiKey.getExpireAt() != null && new Date().after(apiKey.getExpireAt()));
        apiKeyEntity.setRevoked(apiKey.isRevoked());
        apiKeyEntity.setRevokedAt(apiKey.getRevokedAt());
        apiKeyEntity.setUpdatedAt(apiKey.getUpdatedAt());

        apiKeyEntity.setSubscriptions(subscriptionService.findByIdIn(apiKey.getSubscriptions()));

        apiKeyEntity.setApplication(applicationService.findById(executionContext, apiKey.getApplication()));

        apiKeyEntity.setDaysToExpirationOnLastNotification(apiKey.getDaysToExpirationOnLastNotification());

        return apiKeyEntity;
    }

    private ApiKeyCriteria.Builder toApiKeyCriteriaBuilder(ApiKeyQuery query) {
        return new ApiKeyCriteria.Builder()
            .includeRevoked(query.isIncludeRevoked())
            .plans(query.getPlans())
            .from(query.getFrom())
            .to(query.getTo())
            .expireAfter(query.getExpireAfter())
            .expireBefore(query.getExpireBefore());
    }

    private void createAuditLog(
        ExecutionContext executionContext,
        ApiKeyEntity key,
        ApiKey previousApiKey,
        ApiKey.AuditEvent event,
        Date eventDate
    ) {
        key
            .getSubscriptions()
            .forEach(
                subscription -> {
                    Map<Audit.AuditProperties, String> properties = new LinkedHashMap<>();
                    properties.put(API_KEY, key.getKey());
                    properties.put(API, subscription.getApi());
                    properties.put(APPLICATION, key.getApplication().getId());
                    auditService.createApiAuditLog(
                        executionContext,
                        subscription.getApi(),
                        properties,
                        event,
                        eventDate,
                        previousApiKey,
                        key
                    );
                }
            );
    }

    private void triggerNotifierService(
        ExecutionContext executionContext,
        ApiHook apiHook,
        ApiKey key,
        NotificationParamsBuilder paramsBuilder
    ) {
        ApplicationEntity application = applicationService.findById(executionContext, key.getApplication());
        Set<SubscriptionEntity> subscriptions = subscriptionService.findByIdIn(key.getSubscriptions());
        triggerNotifierService(executionContext, apiHook, key, application, subscriptions, paramsBuilder);
    }

    private void triggerNotifierService(
        ExecutionContext executionContext,
        ApiHook apiHook,
        ApiKey key,
        ApplicationEntity application,
        Set<SubscriptionEntity> subscriptions
    ) {
        triggerNotifierService(executionContext, apiHook, key, application, subscriptions, new NotificationParamsBuilder());
    }

    private void triggerNotifierService(
        ExecutionContext executionContext,
        ApiHook apiHook,
        ApiKey key,
        ApplicationEntity application,
        Set<SubscriptionEntity> subscriptions,
        NotificationParamsBuilder paramsBuilder
    ) {
        subscriptions.forEach(
            subscription -> {
                PlanEntity plan = planService.findById(executionContext, subscription.getPlan());
                ApiModelEntity api = apiService.findByIdForTemplates(executionContext, subscription.getApi());
                PrimaryOwnerEntity owner = application.getPrimaryOwner();
                Map<String, Object> params = paramsBuilder.application(application).plan(plan).api(api).owner(owner).apikey(key).build();
                notifierService.trigger(executionContext, apiHook, api.getId(), params);
            }
        );
    }
}
