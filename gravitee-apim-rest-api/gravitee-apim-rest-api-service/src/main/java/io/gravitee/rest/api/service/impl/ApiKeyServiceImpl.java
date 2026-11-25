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
package io.gravitee.rest.api.service.impl;

import static io.gravitee.repository.management.model.ApiKey.AuditEvent.APIKEY_CREATED;
import static io.gravitee.repository.management.model.ApiKey.AuditEvent.APIKEY_EXPIRED;
import static io.gravitee.repository.management.model.ApiKey.AuditEvent.APIKEY_REACTIVATED;
import static io.gravitee.repository.management.model.ApiKey.AuditEvent.APIKEY_RENEWED;
import static io.gravitee.repository.management.model.Audit.AuditProperties.API;
import static io.gravitee.repository.management.model.Audit.AuditProperties.API_KEY;
import static io.gravitee.repository.management.model.Audit.AuditProperties.API_PRODUCT;
import static io.gravitee.repository.management.model.Audit.AuditProperties.APPLICATION;
import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsFirst;
import static java.util.Comparator.nullsLast;
import static java.util.Comparator.reverseOrder;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import io.gravitee.repository.exceptions.DuplicateKeyException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.api.ApiProductsRepository;
import io.gravitee.repository.management.api.search.ApiKeyCriteria;
import io.gravitee.repository.management.model.ApiKey;
import io.gravitee.repository.management.model.ApiProduct;
import io.gravitee.repository.management.model.Audit;
import io.gravitee.repository.management.model.NotificationReferenceType;
import io.gravitee.repository.management.model.SubscriptionReferenceType;
import io.gravitee.rest.api.model.ApiKeyEntity;
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.SubscriptionStatus;
import io.gravitee.rest.api.model.key.ApiKeyQuery;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.model.v4.api.GenericApiModel;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanSecurityType;
import io.gravitee.rest.api.service.ApiKeyGenerator;
import io.gravitee.rest.api.service.ApiKeyService;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.NotifierService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.exceptions.ApiKeyAlreadyActivatedException;
import io.gravitee.rest.api.service.exceptions.ApiKeyAlreadyExistingException;
import io.gravitee.rest.api.service.exceptions.ApiKeyAlreadyExpiredException;
import io.gravitee.rest.api.service.exceptions.ApiKeyCannotBeUsedException;
import io.gravitee.rest.api.service.exceptions.ApiKeyNotFoundException;
import io.gravitee.rest.api.service.exceptions.CustomApiKeyNotAllowedException;
import io.gravitee.rest.api.service.exceptions.InvalidApplicationApiKeyModeException;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import io.gravitee.rest.api.service.exceptions.SubscriptionClosedException;
import io.gravitee.rest.api.service.exceptions.SubscriptionNotActiveException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.notification.ApiHook;
import io.gravitee.rest.api.service.notification.ApiProductTemplateModel;
import io.gravitee.rest.api.service.notification.NotificationParamsBuilder;
import io.gravitee.rest.api.service.v4.ApiTemplateService;
import io.gravitee.rest.api.service.v4.PlanSearchService;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
@Component
public class ApiKeyServiceImpl extends TransactionalService implements ApiKeyService {

    private static final String REPLACEMENT_SHARED_KEY_ID_PREFIX = "shared-api-key-replacement:";
    private static final int CUSTOM_API_KEY_MIN_LENGTH = 8;
    private static final int CUSTOM_API_KEY_MAX_LENGTH = 128;
    /** Same characters and length as {@code @CustomApiKey}. A blank value is not checked: it means generate a key. */
    private static final Pattern CUSTOM_API_KEY_PATTERN = Pattern.compile("[^#%@/;=?|^~, \\\\]*");

    @Lazy
    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private ApiKeyGenerator apiKeyGenerator;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private PlanSearchService planSearchService;

    @Autowired
    private AuditService auditService;

    @Autowired
    private NotifierService notifierService;

    @Autowired
    private ParameterService parameterService;

    @Autowired
    private ApiTemplateService apiTemplateService;

    @Lazy
    @Autowired
    private ApiProductsRepository apiProductsRepository;

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
        if (
            !(subscription.getStatus().equals(io.gravitee.rest.api.model.SubscriptionStatus.ACCEPTED) ||
                subscription.getStatus().equals(io.gravitee.rest.api.model.SubscriptionStatus.PAUSED))
        ) {
            throw new SubscriptionNotActiveException(subscription);
        }
        final GenericPlanEntity genericPlanEntity = planSearchService.findById(executionContext, subscription.getPlan());
        io.gravitee.rest.api.model.v4.plan.PlanSecurityType planSecurityType = null;
        if (genericPlanEntity.getPlanSecurity() != null) {
            planSecurityType = io.gravitee.rest.api.model.v4.plan.PlanSecurityType.valueOfLabel(
                genericPlanEntity.getPlanSecurity().getType()
            );
        }
        if (PlanSecurityType.API_KEY != planSecurityType) {
            throw new TechnicalManagementException("Invalid plan security.");
        }
        try {
            log.debug("Renew API Key for subscription {}", subscription.getId());

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
            throw new TechnicalManagementException(
                String.format("An error occurs while trying to renew an API Key for %s", subscription.getId()),
                ex
            );
        }
    }

    @Override
    public ApiKeyEntity renew(ExecutionContext executionContext, ApplicationEntity application) {
        return renew(executionContext, application, null);
    }

    @Override
    public ApiKeyEntity renew(ExecutionContext executionContext, ApplicationEntity application, String customApiKey) {
        if (!application.hasApiKeySharedMode()) {
            throw new InvalidApplicationApiKeyModeException("Can't renew an API Key on application that doesn't use shared API Key mode");
        }

        String value = customApiKey == null ? null : customApiKey.trim();
        if (!isNotEmpty(value)) {
            return renewGeneratedKey(executionContext, application, null);
        }
        if (
            !parameterService.findAsBoolean(executionContext, Key.PLAN_SECURITY_APIKEY_CUSTOM_ALLOWED, ParameterReferenceType.ENVIRONMENT)
        ) {
            throw new CustomApiKeyNotAllowedException();
        }
        validateCustomApiKeyFormat(value);
        try {
            return renewWithCustomValue(executionContext, application, value);
        } catch (TechnicalException ex) {
            throw new TechnicalManagementException(
                String.format("An error occurs while trying to renew an API Key for application %s", application.getId()),
                ex
            );
        }
    }

    /**
     * Random renewal, and a custom value this application has never used. The two-hour deadline comes from the new
     * key's creation time, which is now.
     */
    private ApiKeyEntity renewGeneratedKey(ExecutionContext executionContext, ApplicationEntity application, String customApiKey) {
        try {
            log.debug("Renew API Key for application {}", application.getId());

            ApiKey newApiKey = generateForApplication(application.getId(), customApiKey, executionContext.getEnvironmentId());
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
            throw new TechnicalManagementException(
                String.format("An error occurs while trying to renew an API Key for application %s", application.getId()),
                ex
            );
        }
    }

    private ApiKeyEntity renewWithCustomValue(ExecutionContext executionContext, ApplicationEntity application, String value)
        throws TechnicalException {
        log.debug("Renew API Key for application {}", application.getId());
        Date now = new Date();
        List<ApiKey> keysWithValue = apiKeyRepository.findByKeyAndEnvironmentId(value, executionContext.getEnvironmentId());
        boolean ownedByAnotherApplication = keysWithValue.stream().anyMatch(apiKey -> !application.getId().equals(apiKey.getApplication()));
        if (ownedByAnotherApplication) {
            throw new ApiKeyCannotBeUsedException();
        }

        List<ApiKey> applicationKeys = apiKeyRepository.findByApplication(application.getId());
        ApiKey currentKey = selectCurrentKey(applicationKeys, now);
        if (currentKey != null && value.equals(currentKey.getKey())) {
            throw new ApiKeyCannotBeUsedException();
        }

        List<ApiKey> rowsOfThisApplication = keysWithValue
            .stream()
            .filter(apiKey -> application.getId().equals(apiKey.getApplication()))
            .toList();
        if (rowsOfThisApplication.isEmpty()) {
            return renewGeneratedKey(executionContext, application, value);
        }

        ApiKey toRestore = selectRowToRestore(rowsOfThisApplication, now);
        toRestore.setRevoked(false);
        toRestore.setRevokedAt(null);
        toRestore.setExpireAt(null);
        toRestore.setUpdatedAt(now);
        ApiKey restored = apiKeyRepository.update(toRestore);

        List<ApiKey> allApiKeys = apiKeyRepository.findByApplication(application.getId());
        expireApiKeysAt(executionContext, allApiKeys, restored, Date.from(now.toInstant().plus(Duration.ofHours(2))));
        addSharedSubscriptions(allApiKeys, restored);

        ApiKeyEntity restoredEntity = convert(executionContext, restored);
        createAuditLog(executionContext, restoredEntity, null, APIKEY_RENEWED, restored.getUpdatedAt());
        triggerNotifierService(
            executionContext,
            ApiHook.APIKEY_RENEWED,
            restored,
            restoredEntity.getApplication(),
            restoredEntity.getSubscriptions()
        );
        return restoredEntity;
    }

    private ApiKey selectCurrentKey(Collection<ApiKey> keys, Date now) {
        return keys
            .stream()
            .filter(apiKey -> isLive(apiKey, now))
            .max(comparing(ApiKey::getExpireAt, nullsLast(naturalOrder())))
            .orElse(null);
    }

    /**
     * A live row that is not the current key is the previous key, still inside its two-hour window. Otherwise the most
     * recently updated revoked or expired row is restored, and no second row is inserted.
     */
    private ApiKey selectRowToRestore(List<ApiKey> candidates, Date now) {
        return candidates
            .stream()
            .max(comparing((ApiKey apiKey) -> isLive(apiKey, now)).thenComparing(ApiKey::getUpdatedAt, nullsFirst(naturalOrder())))
            .orElseThrow();
    }

    private boolean isLive(ApiKey apiKey, Date now) {
        return !apiKey.isRevoked() && (apiKey.getExpireAt() == null || !apiKey.getExpireAt().before(now));
    }

    /**
     * The gateway keeps one cache entry per API and key value. Writing another row that still carries the active value
     * publishes that row by {@code updatedAt} and can unload the value that this renewal just made current.
     */
    private boolean sameKeyValue(ApiKey apiKey, ApiKey activeApiKey) {
        return apiKey.getKey() != null && apiKey.getKey().equals(activeApiKey.getKey());
    }

    private void validateCustomApiKeyFormat(String value) {
        if (value.length() < CUSTOM_API_KEY_MIN_LENGTH || value.length() > CUSTOM_API_KEY_MAX_LENGTH) {
            throw new InvalidDataException("Should have length between 8 and 128 characters");
        }
        if (!CUSTOM_API_KEY_PATTERN.matcher(value).matches()) {
            throw new InvalidDataException("Should not contain: ^ # % @ \\ / ; = ? | ~ , (space)");
        }
    }

    private void expireApiKeys(ExecutionContext executionContext, Collection<ApiKey> apiKeys, ApiKey activeApiKey)
        throws TechnicalException {
        Instant expirationInst = activeApiKey.getCreatedAt().toInstant().plus(Duration.ofHours(2));
        expireApiKeysAt(executionContext, apiKeys, activeApiKey, Date.from(expirationInst));
    }

    private void expireApiKeysAt(ExecutionContext executionContext, Collection<ApiKey> apiKeys, ApiKey activeApiKey, Date expirationDate)
        throws TechnicalException {
        for (ApiKey apiKey : apiKeys) {
            if (apiKey.equals(activeApiKey) || sameKeyValue(apiKey, activeApiKey)) {
                continue;
            }
            ApiKeyEntity apiKeyEntity = convert(executionContext, apiKey);
            if (!apiKeyEntity.isExpired()) {
                updateExpirationDate(executionContext, expirationDate, apiKeyEntity, apiKey);
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
        try {
            List<ApiKey> keys = apiKeyRepository.findByApplication(application.getId());
            if (keys == null || keys.isEmpty()) {
                return generate(executionContext, subscription, customApiKey);
            }
            Date now = new Date();
            if (keys.stream().anyMatch(apiKey -> isLive(apiKey, now))) {
                return attachSubscriptionToExistingKeys(executionContext, application, subscription, customApiKey);
            }
            return createSharedKeyWhenNoneIsLive(executionContext, application, subscription, keys);
        } catch (TechnicalException ex) {
            throw new TechnicalManagementException(
                String.format("An error occurs while trying to generate an API Key for %s", subscription),
                ex
            );
        }
    }

    /**
     * While a live key exists, a new subscription is attached to every key of the application, including the previous
     * key during the two-hour overlap and keys that are already revoked.
     */
    private ApiKeyEntity attachSubscriptionToExistingKeys(
        ExecutionContext executionContext,
        ApplicationEntity application,
        SubscriptionEntity subscription,
        String customApiKey
    ) {
        return findByApplication(executionContext, application.getId())
            .stream()
            .map(apiKey -> addSubscription(executionContext, apiKey, subscription))
            .max(comparing(ApiKeyEntity::isRevoked, reverseOrder()).thenComparing(ApiKeyEntity::getExpireAt, nullsLast(naturalOrder())))
            .orElseGet(() -> generate(executionContext, subscription, customApiKey));
    }

    /**
     * Every existing key is revoked or expired. The new subscription is not attached to those rows, and none of them is
     * turned back on. One new shared key, with no expiry, carries the subscriptions already stored on the application
     * plus this subscription. A value supplied on the subscription is ignored: a chosen value is set by renewing the
     * application key.
     * <p>
     * The id is derived from the application and every existing key id, so two accepts that read the same keys insert
     * one key, and a later accept after that key has died gets a different id. A duplicate insert is reused only when
     * the existing row is live. A dead row is left alone and a new key is created.
     */
    private ApiKeyEntity createSharedKeyWhenNoneIsLive(
        ExecutionContext executionContext,
        ApplicationEntity application,
        SubscriptionEntity subscription,
        List<ApiKey> existingKeys
    ) throws TechnicalException {
        Set<String> subscriptionIds = new LinkedHashSet<>();
        for (ApiKey existing : existingKeys) {
            if (existing.getSubscriptions() != null) {
                subscriptionIds.addAll(existing.getSubscriptions());
            }
        }
        subscriptionIds.add(subscription.getId());

        String replacementId = replacementSharedKeyId(application.getId(), existingKeys);
        try {
            return storeSharedKeyWhenNoneIsLive(executionContext, application, subscriptionIds, replacementId);
        } catch (TechnicalException e) {
            if (!isDuplicateKey(e)) {
                throw e;
            }
            return reuseLiveReplacementOrCreateAnother(executionContext, application, subscription, subscriptionIds, replacementId);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            return reuseLiveReplacementOrCreateAnother(executionContext, application, subscription, subscriptionIds, replacementId);
        }
    }

    private ApiKeyEntity reuseLiveReplacementOrCreateAnother(
        ExecutionContext executionContext,
        ApplicationEntity application,
        SubscriptionEntity subscription,
        Set<String> subscriptionIds,
        String replacementId
    ) throws TechnicalException {
        Optional<ApiKey> existing = apiKeyRepository
            .findById(replacementId)
            .filter(row -> application.getId().equals(row.getApplication()));
        if (existing.isPresent() && isLive(existing.get(), new Date())) {
            log.debug("The shared API Key of application {} was created by another request meanwhile, reusing it", application.getId());
            ApiKey reused = apiKeyRepository
                .addSubscription(existing.get().getId(), subscription.getId())
                .orElseThrow(ApiKeyNotFoundException::new);
            return convert(executionContext, reused);
        }
        return storeSharedKeyWhenNoneIsLive(executionContext, application, subscriptionIds, null);
    }

    private static boolean isDuplicateKey(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof DuplicateKeyException || cause instanceof org.springframework.dao.DuplicateKeyException) {
                return true;
            }
        }
        return false;
    }

    private ApiKeyEntity storeSharedKeyWhenNoneIsLive(
        ExecutionContext executionContext,
        ApplicationEntity application,
        Set<String> subscriptionIds,
        String replacementId
    ) throws TechnicalException {
        ApiKey apiKey = generateForApplication(application.getId(), null, executionContext.getEnvironmentId());
        if (replacementId != null) {
            apiKey.setId(replacementId);
        }
        apiKey.setSubscriptions(new ArrayList<>(subscriptionIds));
        apiKey = apiKeyRepository.create(apiKey);
        ApiKeyEntity apiKeyEntity = convert(executionContext, apiKey);
        createAuditLog(executionContext, apiKeyEntity, null, APIKEY_CREATED, apiKey.getCreatedAt());
        return apiKeyEntity;
    }

    private static String replacementSharedKeyId(String applicationId, List<ApiKey> existingKeys) {
        String keyIds = existingKeys.stream().map(ApiKey::getId).filter(Objects::nonNull).sorted().collect(joining(","));
        return UUID.nameUUIDFromBytes(
            (REPLACEMENT_SHARED_KEY_ID_PREFIX + applicationId + ":" + keyIds).getBytes(StandardCharsets.UTF_8)
        ).toString();
    }

    private ApiKeyEntity addSubscription(ExecutionContext executionContext, ApiKeyEntity apiKeyEntity, SubscriptionEntity subscription) {
        try {
            return convert(
                executionContext,
                apiKeyRepository.addSubscription(apiKeyEntity.getId(), subscription.getId()).orElseThrow(ApiKeyNotFoundException::new)
            );
        } catch (TechnicalException e) {
            throw new TechnicalManagementException("An error occurred while trying to a add subscription to API Key");
        }
    }

    private ApiKeyEntity generate(ExecutionContext executionContext, SubscriptionEntity subscription, String customApiKey) {
        try {
            log.debug("Generate an API Key for subscription {}", subscription);

            ApiKey apiKey = generateForSubscription(executionContext, subscription, customApiKey);
            apiKey = apiKeyRepository.create(apiKey);

            //TODO: Send a notification to the application owner

            ApiKeyEntity apiKeyEntity = convert(executionContext, apiKey);
            // Audit
            createAuditLog(executionContext, apiKeyEntity, null, APIKEY_CREATED, apiKey.getCreatedAt());

            return apiKeyEntity;
        } catch (TechnicalException ex) {
            throw new TechnicalManagementException(
                String.format("An error occurs while trying to generate an API Key for %s", subscription),
                ex
            );
        }
    }

    /**
     * Generate an {@link ApiKey} from a subscription. If no custom API Key, then generate a new one.
     *
     * @param executionContext
     * @param subscription
     * @param customApiKey
     * @return An API Key
     */
    private ApiKey generateForSubscription(ExecutionContext executionContext, SubscriptionEntity subscription, String customApiKey) {
        if (isNotEmpty(customApiKey) && !canCreate(executionContext, customApiKey, subscription)) {
            throw new ApiKeyAlreadyExistingException();
        }

        Date now = new Date();
        if (subscription.getEndingAt() != null && subscription.getEndingAt().before(now)) {
            throw new SubscriptionClosedException(subscription.getId());
        }

        ApiKey apiKey = generateForApplication(subscription.getApplication(), customApiKey, executionContext.getEnvironmentId());
        apiKey.setSubscriptions(List.of(subscription.getId()));

        // By default, the API Key will expire when subscription is closed
        apiKey.setExpireAt(subscription.getEndingAt());

        return apiKey;
    }

    /**
     * Generate an {@link ApiKey} for an application. Generates a new random key value.
     *
     * @param application
     * @param environmentId
     * @return An API Key
     */
    private ApiKey generateForApplication(ApplicationEntity application, String environmentId) {
        return generateForApplication(application.getId(), null, environmentId);
    }

    /**
     * Generate an {@link ApiKey} for an application. If no custom API Key, then generate a new one.
     *
     * @param application
     * @param customApiKey
     * @param environmentId
     * @return An API Key
     */
    private ApiKey generateForApplication(String application, String customApiKey, String environmentId) {
        ApiKey apiKey = new ApiKey();
        apiKey.setId(UuidString.generateRandom());
        apiKey.setApplication(application);
        apiKey.setEnvironmentId(environmentId);
        apiKey.setCreatedAt(new Date());
        apiKey.setUpdatedAt(apiKey.getCreatedAt());
        apiKey.setKey(isNotEmpty(customApiKey) ? customApiKey : apiKeyGenerator.generate());
        return apiKey;
    }

    @Override
    public ApiKeyEntity reactivate(ExecutionContext executionContext, ApiKeyEntity apiKeyEntity) {
        try {
            ApiKey key = apiKeyRepository.findById(apiKeyEntity.getId()).orElseThrow(ApiKeyNotFoundException::new);

            log.debug("Reactivate API Key id {}", apiKeyEntity.getId());

            if (!key.isRevoked() && !convert(executionContext, key).isExpired()) {
                throw new ApiKeyAlreadyActivatedException();
            }

            ApiKey previousApiKey = new ApiKey(key);
            key.setRevoked(false);
            key.setUpdatedAt(new Date());
            key.setRevokedAt(null);

            // If this is not a shared API Key,
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
            throw new TechnicalManagementException("An error occurs while trying to reactivate an API Key", ex);
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
            throw new TechnicalManagementException(message, e);
        }
    }

    @Override
    public List<ApiKeyEntity> findByKey(ExecutionContext executionContext, String apiKey) {
        try {
            log.debug("Find API Keys by key");
            return apiKeyRepository
                .findByKey(apiKey)
                .stream()
                .map(apiKey1 -> convert(executionContext, apiKey1))
                .collect(toList());
        } catch (TechnicalException e) {
            throw new TechnicalManagementException("An error occurs while finding API Keys", e);
        }
    }

    @Override
    public List<ApiKeyEntity> findByKeyAndEnvironmentId(ExecutionContext executionContext, String apiKey) {
        try {
            log.debug("Find API Keys by key and environment id");
            return apiKeyRepository
                .findByKeyAndEnvironmentId(apiKey, executionContext.getEnvironmentId())
                .stream()
                .map(apiKey1 -> convert(executionContext, apiKey1))
                .collect(toList());
        } catch (TechnicalException e) {
            throw new TechnicalManagementException("An error occurs while finding API Keys", e);
        }
    }

    @Override
    public List<ApiKeyEntity> findBySubscription(ExecutionContext executionContext, String subscription) {
        try {
            log.debug("Find API Keys for subscription {}", subscription);

            SubscriptionEntity subscriptionEntity = subscriptionService.findById(subscription);
            Set<ApiKey> keys = apiKeyRepository.findBySubscription(subscriptionEntity.getId());
            return keys
                .stream()
                .map(apiKey -> convert(executionContext, apiKey))
                .sorted((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()))
                .collect(toList());
        } catch (TechnicalException ex) {
            throw new TechnicalManagementException(
                String.format("An error occurs while finding API Keys for subscription %s", subscription),
                ex
            );
        }
    }

    @Override
    public ApiKeyEntity findByKeyAndApi(ExecutionContext executionContext, String apiKey, String apiId) {
        try {
            log.debug("Find an API Key by key for API {}", apiId);
            ApiKey key = apiKeyRepository.findByKeyAndApi(apiKey, apiId).orElseThrow(ApiKeyNotFoundException::new);
            return convert(executionContext, key);
        } catch (TechnicalException ex) {
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
            throw new TechnicalManagementException(
                String.format("An error occurs while trying to find API Keys for application %s", applicationId),
                ex
            );
        }
    }

    @Override
    public void deleteByApplication(String applicationId) {
        try {
            List<String> deletedIds = apiKeyRepository.deleteByApplication(applicationId);
            log.debug("API Keys {} of application {} deleted", deletedIds, applicationId);
        } catch (TechnicalException ex) {
            throw new TechnicalManagementException(
                String.format("An error occurs while trying to delete API Keys of application %s", applicationId),
                ex
            );
        }
    }

    @Override
    public ApiKeyEntity update(ExecutionContext executionContext, ApiKeyEntity apiKeyEntity) {
        try {
            log.debug("Update API Key with id {}", apiKeyEntity.getId());
            ApiKey key = apiKeyRepository.findById(apiKeyEntity.getId()).orElseThrow(ApiKeyNotFoundException::new);

            checkApiKeyExpired(executionContext, key);

            key.setEnvironmentId(apiKeyEntity.getEnvironmentId());
            key.setSubscriptions(apiKeyEntity.getSubscriptionIds());
            key.setPaused(apiKeyEntity.isPaused());
            updateExpirationDate(executionContext, apiKeyEntity.getExpireAt(), apiKeyEntity, key);

            return convert(executionContext, key);
        } catch (TechnicalException ex) {
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
                .map(dbApiKey -> {
                    dbApiKey.setDaysToExpirationOnLastNotification(value);
                    try {
                        return apiKeyRepository.update(dbApiKey);
                    } catch (TechnicalException ex) {
                        throw new TechnicalManagementException(
                            String.format("An error occurs while trying to update ApiKey with id %s", dbApiKey.getId()),
                            ex
                        );
                    }
                })
                .map(apiKey -> convert(executionContext, apiKey))
                .orElseThrow(ApiKeyNotFoundException::new);
        } catch (TechnicalException ex) {
            throw new TechnicalManagementException("An error occurs while trying to update apiKey", ex);
        }
    }

    @Override
    public boolean canCreate(ExecutionContext executionContext, String apiKeyValue, SubscriptionEntity subscription) {
        String referenceId = subscription.getReferenceId() != null ? subscription.getReferenceId() : subscription.getApi();
        String referenceType = subscription.getReferenceType() != null ? subscription.getReferenceType() : "API";
        return canCreate(executionContext, apiKeyValue, referenceId, referenceType, subscription.getApplication());
    }

    @Override
    public boolean canCreate(
        ExecutionContext executionContext,
        String apiKey,
        String referenceId,
        String referenceType,
        String applicationId
    ) {
        log.debug(
            "Check if an API Key can be created for reference {} (type {}) and application {}",
            referenceId,
            referenceType,
            applicationId
        );
        return findByKeyAndEnvironmentId(executionContext, apiKey)
            .stream()
            .noneMatch(existingKey -> isConflictingKeyForReference(existingKey, referenceId, referenceType, applicationId));
    }

    private boolean isConflictingKeyForReference(ApiKeyEntity existingKey, String referenceId, String referenceType, String applicationId) {
        if (!existingKey.getApplication().getId().equals(applicationId)) {
            return true;
        }
        return existingKey
            .getSubscriptions()
            .stream()
            .anyMatch(
                sub ->
                    (sub.getReferenceId() != null &&
                        sub.getReferenceType() != null &&
                        referenceId.equals(sub.getReferenceId()) &&
                        referenceType.equals(sub.getReferenceType())) ||
                    (SubscriptionReferenceType.API.name().equals(referenceType) && referenceId.equals(sub.getApi()))
            );
    }

    @Override
    public Collection<ApiKeyEntity> search(ExecutionContext executionContext, ApiKeyQuery query) {
        try {
            log.debug("Search API Keys {}", query);

            ApiKeyCriteria apiKeyCriteria = toApiKeyCriteria(query);

            return apiKeyRepository
                .findByCriteria(apiKeyCriteria)
                .stream()
                .map(apiKey -> convert(executionContext, apiKey))
                .collect(toList());
        } catch (TechnicalException ex) {
            throw new TechnicalManagementException(String.format("An error occurs while trying to search API Keys: {}", query), ex);
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

    private void updateExpirationDate(ExecutionContext executionContext, Date expirationDate, ApiKeyEntity apiKeyEntity, ApiKey key)
        throws TechnicalException {
        final Date now = new Date();

        key.setUpdatedAt(now);
        if (key.isRevoked()) {
            apiKeyRepository.update(key);
            return;
        }

        // If the expiration date is before now, then the key is already expired
        if (expirationDate != null && expirationDate.before(now)) {
            expirationDate = now;
        }

        // If the API Key is not shared
        // The expired date must be <= than the subscription end date
        if (
            apiKeyEntity.getApplication() != null &&
            !apiKeyEntity.getApplication().hasApiKeySharedMode() &&
            !key.getSubscriptions().isEmpty()
        ) {
            SubscriptionEntity subscription = subscriptionService.findById(key.getSubscriptions().get(0));
            if (
                subscription.getEndingAt() != null && (expirationDate == null || subscription.getEndingAt().compareTo(expirationDate) < 0)
            ) {
                expirationDate = subscription.getEndingAt();
            }
        }

        ApiKey oldkey = new ApiKey(key);
        key.setExpireAt(expirationDate);
        key.setDaysToExpirationOnLastNotification(null);
        apiKeyRepository.update(key);

        // Audit
        createAuditLog(executionContext, convert(executionContext, key), oldkey, APIKEY_EXPIRED, key.getUpdatedAt());

        // If there is an expiration date then notify
        if (expirationDate != null) {
            NotificationParamsBuilder paramsBuilder = new NotificationParamsBuilder();
            if (key.getExpireAt() != null && now.before(key.getExpireAt())) {
                paramsBuilder.expirationDate(key.getExpireAt());
            }
            triggerNotifierService(executionContext, ApiHook.APIKEY_EXPIRED, key, paramsBuilder);
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
        apiKeyEntity.setEnvironmentId(apiKey.getEnvironmentId());

        apiKeyEntity.setSubscriptions(subscriptionService.findByIdIn(apiKey.getSubscriptions()));

        apiKeyEntity.setApplication(applicationService.findById(executionContext, apiKey.getApplication()));

        apiKeyEntity.setDaysToExpirationOnLastNotification(apiKey.getDaysToExpirationOnLastNotification());

        if (apiKey.getKey() != null) {
            // Hash the API Key as used for authentication in the Kafka Gateway
            apiKeyEntity.setHash(DigestUtils.md5DigestAsHex(apiKey.getKey().getBytes()));
        }

        return apiKeyEntity;
    }

    private ApiKeyCriteria toApiKeyCriteria(ApiKeyQuery query) {
        ApiKeyCriteria.ApiKeyCriteriaBuilder apiKeyCriteriaBuilder = ApiKeyCriteria.builder()
            .includeRevoked(query.isIncludeRevoked())
            .includeFederated(query.isIncludeFederated())
            .from(query.getFrom())
            .to(query.getTo())
            .expireAfter(query.getExpireAfter())
            .expireBefore(query.getExpireBefore());
        if (query.getSubscriptions() != null) {
            apiKeyCriteriaBuilder.subscriptions(query.getSubscriptions());
        }
        if (query.getEnvironmentIds() != null) {
            apiKeyCriteriaBuilder.environments(query.getEnvironmentIds());
        }
        return apiKeyCriteriaBuilder.build();
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
            .forEach(subscription -> {
                boolean isApiProduct = isApiProductSubscription(subscription);
                String referenceId = subscription.getReferenceId() != null ? subscription.getReferenceId() : subscription.getApi();
                String applicationId = key.getApplication() != null ? key.getApplication().getId() : null;

                Map<Audit.AuditProperties, String> properties = new LinkedHashMap<>();
                // Audit records are readable by anyone holding the audit permission, so they identify the API key
                // by its id: the key value itself is a credential that can be replayed against the gateway.
                properties.put(API_KEY, key.getId());
                properties.put(isApiProduct ? API_PRODUCT : API, referenceId);
                properties.put(APPLICATION, applicationId);

                AuditService.AuditLogData auditLogData = AuditService.AuditLogData.builder()
                    .properties(properties)
                    .event(event)
                    .createdAt(eventDate)
                    .oldValue(previousApiKey == null ? null : previousApiKey.toBuilder().key(null).build())
                    // hash is the MD5 of the key and doubles as a credential for Native Kafka
                    // authentication, so it leaves the audit record along with the key itself. The
                    // repository ApiKey used as the old value carries no such field.
                    .newValue(key.toBuilder().key(null).hash(null).build())
                    .build();

                if (isApiProduct) {
                    auditService.createApiProductAuditLog(executionContext, auditLogData, referenceId);
                } else {
                    auditService.createApiAuditLog(executionContext, auditLogData, referenceId);
                }
            });
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
        subscriptions.forEach(subscription -> {
            if (isApiProductSubscription(subscription)) {
                triggerNotifierServiceForApiProductSubscription(executionContext, apiHook, key, application, subscription);
                return;
            }
            GenericPlanEntity genericPlanEntity = planSearchService.findById(executionContext, subscription.getPlan());
            GenericApiModel genericApiModel = apiTemplateService.findByIdForTemplates(executionContext, subscription.getApi());
            PrimaryOwnerEntity owner = application.getPrimaryOwner();
            Map<String, Object> params = paramsBuilder
                .application(application)
                .plan(genericPlanEntity)
                .api(genericApiModel)
                .owner(owner)
                .apikey(key)
                .build();
            notifierService.trigger(executionContext, apiHook, genericApiModel.getId(), params);
        });
    }

    private void triggerNotifierServiceForApiProductSubscription(
        ExecutionContext executionContext,
        ApiHook apiHook,
        ApiKey key,
        ApplicationEntity application,
        SubscriptionEntity subscription
    ) {
        String apiProductId = subscription.getReferenceId();
        try {
            GenericPlanEntity genericPlanEntity = planSearchService.findById(executionContext, subscription.getPlan());
            Optional<ApiProduct> apiProductOpt = apiProductsRepository.findById(apiProductId);
            if (apiProductOpt.isEmpty()) {
                log.debug("API Product [{}] not found, skipping API key notification", apiProductId);
                return;
            }
            ApiProduct apiProduct = apiProductOpt.get();
            PrimaryOwnerEntity applicationOwner = application.getPrimaryOwner();
            ApiProductTemplateModel apiProductModel = ApiProductTemplateModel.builder()
                .id(apiProduct.getId())
                .name(apiProduct.getName())
                .version(apiProduct.getVersion() != null ? apiProduct.getVersion() : "")
                .primaryOwner(applicationOwner)
                .build();
            NotificationParamsBuilder freshBuilder = new NotificationParamsBuilder()
                .apiProduct(apiProductModel)
                .plan(genericPlanEntity)
                .application(application)
                .owner(applicationOwner)
                .apikey(key);

            // Only include expirationDate when the key expires in the future.
            // The email template renders "will expire on <date>" when present,
            // and "has expired" when absent.
            final Date now = new Date();
            if (key.getExpireAt() != null && now.before(key.getExpireAt())) {
                freshBuilder.expirationDate(key.getExpireAt());
            }
            notifierService.trigger(executionContext, apiHook, NotificationReferenceType.API_PRODUCT, apiProductId, freshBuilder.build());
        } catch (TechnicalException e) {
            log.error("Error triggering API key notification for API Product subscription {}", subscription.getId(), e);
        }
    }

    private boolean isApiProductSubscription(SubscriptionEntity subscription) {
        var referenceType = subscription.getReferenceType();
        return referenceType != null && SubscriptionReferenceType.API_PRODUCT.name().equals(referenceType);
    }
}
