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

package io.gravitee.apim.core.api_key.domain_service;

import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsLast;
import static java.util.stream.Collectors.joining;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.api_key.crud_service.ApiKeyCrudService;
import io.gravitee.apim.core.api_key.exception.ApiKeyIdAlreadyExistsException;
import io.gravitee.apim.core.api_key.model.ApiKeyEntity;
import io.gravitee.apim.core.api_key.query_service.ApiKeyQueryService;
import io.gravitee.apim.core.application.crud_service.ApplicationCrudService;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.ApiAuditLogEntity;
import io.gravitee.apim.core.audit.model.ApiProductAuditLogEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.audit.model.event.ApiKeyAuditEvent;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.apim.core.subscription.model.SubscriptionReferenceType;
import io.gravitee.repository.management.model.ApiKey;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.gravitee.rest.api.service.exceptions.ApiKeyAlreadyExistingException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

@DomainService
@CustomLog
@RequiredArgsConstructor
public class GenerateApiKeyDomainService {

    private static final String REPLACEMENT_SHARED_KEY_ID_PREFIX = "shared-api-key-replacement:";

    private final ApiKeyCrudService apiKeyCrudService;
    private final ApiKeyQueryService apiKeyQueryService;
    private final ApplicationCrudService applicationCrudService;
    private final AuditDomainService auditService;

    public ApiKeyEntity generate(SubscriptionEntity subscription, AuditInfo auditInfo, String customApiKey) {
        var app = applicationCrudService.findById(subscription.getApplicationId(), auditInfo.environmentId());
        return generate(subscription, app, auditInfo, customApiKey, false);
    }

    public ApiKeyEntity generateForFederated(SubscriptionEntity subscription, AuditInfo auditInfo, String customApiKey) {
        var app = applicationCrudService.findById(subscription.getApplicationId(), auditInfo.environmentId());
        return generate(subscription, app, auditInfo, customApiKey, true);
    }

    private ApiKeyEntity generate(
        SubscriptionEntity subscription,
        BaseApplicationEntity application,
        AuditInfo auditInfo,
        String apiKeyValue,
        boolean federated
    ) {
        if (!application.hasApiKeySharedMode()) {
            return generate(auditInfo, subscription, apiKeyValue, federated);
        }
        return findOrGenerate(auditInfo, application, subscription, apiKeyValue, federated);
    }

    private ApiKeyEntity generate(AuditInfo auditInfo, SubscriptionEntity subscription, String apiKeyValue, boolean federated) {
        log.debug("Generate an API Key for subscription {}", subscription);

        ApiKeyEntity apiKey = generateForSubscription(subscription, apiKeyValue, federated);
        apiKeyCrudService.create(apiKey);

        // Audit
        createAuditLog(apiKey, subscription, auditInfo);

        return apiKey;
    }

    /**
     * Generate an {@link ApiKey} from a subscription. If no custom API Key, then generate a new one.
     *
     * @param subscription The subscription
     * @param customApiKey The custom key to use
     * @param federated Flag indicating that we handle a federated subscription
     * @return An API Key
     */
    private ApiKeyEntity generateForSubscription(SubscriptionEntity subscription, String customApiKey, boolean federated) {
        if (federated) {
            return ApiKeyEntity.generateForFederatedSubscription(subscription, customApiKey);
        }

        if (Objects.nonNull(customApiKey) && !customApiKey.isEmpty()) {
            if (!isKeyExistFor(customApiKey, subscription)) {
                throw new ApiKeyAlreadyExistingException();
            }
            return ApiKeyEntity.generateForSubscription(subscription, customApiKey);
        }

        return ApiKeyEntity.generateForSubscription(subscription);
    }

    private boolean isKeyExistFor(String apiKeyValue, SubscriptionEntity subscription) {
        String referenceId = subscription.getReferenceId();
        String referenceType = subscription.getReferenceType() != null ? subscription.getReferenceType().name() : null;
        log.debug(
            "Check if an API Key can be created for reference {} (type {}) and application {}",
            referenceId,
            referenceType,
            subscription.getApplicationId()
        );

        if (referenceId != null && referenceType != null) {
            return apiKeyQueryService.findByKeyAndReferenceIdAndReferenceType(apiKeyValue, referenceId, referenceType).isEmpty();
        }
        return apiKeyQueryService.findByKeyAndApiId(apiKeyValue, subscription.getApiId()).isEmpty();
    }

    private ApiKeyEntity findOrGenerate(
        AuditInfo auditInfo,
        BaseApplicationEntity application,
        SubscriptionEntity subscription,
        String customApiKey,
        boolean federated
    ) {
        List<ApiKeyEntity> keys = apiKeyQueryService.findByApplication(application.getId()).toList();
        if (!federated && !keys.isEmpty() && keys.stream().noneMatch(ApiKeyEntity::canBeRevoked)) {
            return generateSharedKeyWhenNoneIsLive(auditInfo, keys, subscription);
        }
        boolean anyNotRevoked = keys.stream().anyMatch(apiKey -> !apiKey.isRevoked());
        return keys
            .stream()
            .filter(apiKey -> !anyNotRevoked || !apiKey.isRevoked())
            .peek(apiKey -> addSubscription(apiKey, subscription))
            .max(comparing(ApiKeyEntity::getExpireAt, nullsLast(naturalOrder())))
            .orElseGet(() -> generate(auditInfo, subscription, customApiKey, federated));
    }

    /**
     * Every key of the application is revoked or expired. The new subscription is not attached to those rows, and none
     * of them is turned back on. One new shared key, with no expiry, carries the subscriptions already stored on the
     * application plus this subscription. A value supplied on the subscription is ignored.
     * <p>
     * The id is derived from the application and every existing key id. A random id would let two accepts handled at the
     * same time each insert a live key. Deriving it from one dead key repeats that id after the replacement itself
     * expires. A duplicate insert is the other request's key only when that row is live; a dead row is left alone and a
     * new key is created.
     */
    private ApiKeyEntity generateSharedKeyWhenNoneIsLive(
        AuditInfo auditInfo,
        List<ApiKeyEntity> existingKeys,
        SubscriptionEntity subscription
    ) {
        String replacementId = replacementSharedKeyId(subscription.getApplicationId(), existingKeys);
        try {
            return createSharedKeyWhenNoneIsLive(auditInfo, existingKeys, subscription, replacementId);
        } catch (ApiKeyIdAlreadyExistsException exception) {
            Optional<ApiKeyEntity> existing = apiKeyQueryService
                .findByApplication(subscription.getApplicationId())
                .filter(apiKey -> replacementId.equals(apiKey.getId()))
                .findAny();
            if (existing.isPresent() && existing.get().canBeRevoked()) {
                log.debug(
                    "The shared API Key of application {} was created by another request meanwhile, reusing it",
                    subscription.getApplicationId()
                );
                return apiKeyCrudService
                    .addSubscription(replacementId, subscription.getId())
                    .orElseThrow(() ->
                        new TechnicalDomainException(
                            "Cannot create the shared API Key of application [" +
                                subscription.getApplicationId() +
                                "]: its id [" +
                                replacementId +
                                "] is already used, but that API Key is gone",
                            exception
                        )
                    );
            }
            return createSharedKeyWhenNoneIsLive(auditInfo, existingKeys, subscription, null);
        }
    }

    private ApiKeyEntity createSharedKeyWhenNoneIsLive(
        AuditInfo auditInfo,
        List<ApiKeyEntity> existingKeys,
        SubscriptionEntity subscription,
        String replacementId
    ) {
        ApiKeyEntity generated = ApiKeyEntity.generateForSubscription(subscription);
        ApiKeyEntity toStore = replacementId == null
            ? generated.toBuilder().expireAt(null).build()
            : generated.toBuilder().id(replacementId).expireAt(null).build();
        ApiKeyEntity created = apiKeyCrudService.create(toStore);
        createAuditLog(created, subscription, auditInfo);
        for (ApiKeyEntity existing : existingKeys) {
            if (existing.getSubscriptions() == null) {
                continue;
            }
            for (String subscriptionId : existing.getSubscriptions()) {
                if (subscription.getId().equals(subscriptionId) || created.hasSubscription(subscriptionId)) {
                    continue;
                }
                created = apiKeyCrudService.addSubscription(created.getId(), subscriptionId).orElse(created);
            }
        }
        return created;
    }

    private static String replacementSharedKeyId(String applicationId, List<ApiKeyEntity> existingKeys) {
        String keyIds = existingKeys.stream().map(ApiKeyEntity::getId).filter(Objects::nonNull).sorted().collect(joining(","));
        return UUID.nameUUIDFromBytes(
            (REPLACEMENT_SHARED_KEY_ID_PREFIX + applicationId + ":" + keyIds).getBytes(StandardCharsets.UTF_8)
        ).toString();
    }

    /**
     * A key that is not revoked gets the subscription, an expired one included, so that the previous key keeps working on
     * the new subscription during the overlap that follows a shared key renewal. A revoked key is left unchanged while a
     * key that is not revoked exists: a revoked duplicate of that key would otherwise be loaded on redeploy and remove the
     * live key from the gateway cache. When every key is revoked, a federated subscription is attached to those keys.
     */
    private void addSubscription(ApiKeyEntity apiKey, SubscriptionEntity subscription) {
        apiKeyCrudService.update(apiKey.addSubscription(subscription.getId()));
    }

    private void createAuditLog(ApiKeyEntity createdApiKeyEntity, SubscriptionEntity subscription, AuditInfo auditInfo) {
        boolean isApiProduct = SubscriptionReferenceType.API_PRODUCT.equals(subscription.getReferenceType());
        String referenceId = isApiProduct ? subscription.getReferenceId() : subscription.getApiId();

        Map<AuditProperties, String> properties = new HashMap<>();
        // Audit records are readable by anyone holding the audit permission, so they identify the API key by its
        // id: the key value itself is a credential that can be replayed against the gateway.
        properties.put(AuditProperties.API_KEY, createdApiKeyEntity.getId());
        if (referenceId != null) {
            properties.put(isApiProduct ? AuditProperties.API_PRODUCT : AuditProperties.API, referenceId);
        }
        properties.put(AuditProperties.APPLICATION, subscription.getApplicationId());

        var auditedApiKey = createdApiKeyEntity.toBuilder().key(null).build();

        if (isApiProduct) {
            auditService.createApiProductAuditLog(
                ApiProductAuditLogEntity.builder()
                    .organizationId(auditInfo.organizationId())
                    .environmentId(auditInfo.environmentId())
                    .apiProductId(referenceId)
                    .event(ApiKeyAuditEvent.APIKEY_CREATED)
                    .actor(auditInfo.actor())
                    .oldValue(null)
                    .newValue(auditedApiKey)
                    .createdAt(createdApiKeyEntity.getCreatedAt())
                    .properties(properties)
                    .build()
            );
        } else {
            auditService.createApiAuditLog(
                ApiAuditLogEntity.builder()
                    .organizationId(auditInfo.organizationId())
                    .environmentId(auditInfo.environmentId())
                    .apiId(referenceId)
                    .event(ApiKeyAuditEvent.APIKEY_CREATED)
                    .actor(auditInfo.actor())
                    .oldValue(null)
                    .newValue(auditedApiKey)
                    .createdAt(createdApiKeyEntity.getCreatedAt())
                    .properties(properties)
                    .build()
            );
        }
    }
}
