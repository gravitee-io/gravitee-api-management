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
import static java.util.Comparator.reverseOrder;

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
import io.gravitee.apim.core.parameters.model.ParameterContext;
import io.gravitee.apim.core.parameters.query_service.ParametersQueryService;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.apim.core.subscription.model.SubscriptionReferenceType;
import io.gravitee.repository.management.model.ApiKey;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.service.exceptions.ApiKeyAlreadyExistingException;
import io.gravitee.rest.api.service.exceptions.ApiKeyNotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

@DomainService
@CustomLog
@RequiredArgsConstructor
public class GenerateApiKeyDomainService {

    private static final String FIRST_SHARED_KEY_ID_PREFIX = "shared-api-key:";

    private final ApiKeyCrudService apiKeyCrudService;
    private final ApiKeyQueryService apiKeyQueryService;
    private final ApplicationCrudService applicationCrudService;
    private final AuditDomainService auditService;
    private final ParametersQueryService parametersQueryService;

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
        return generate(auditInfo, subscription, apiKeyValue, federated, UnaryOperator.identity());
    }

    /**
     * @param withId gives the key to create its id, or keeps the random one
     * @throws ApiKeyIdAlreadyExistsException when the id given by {@code withId} is already used
     */
    private ApiKeyEntity generate(
        AuditInfo auditInfo,
        SubscriptionEntity subscription,
        String apiKeyValue,
        boolean federated,
        UnaryOperator<ApiKeyEntity> withId
    ) {
        log.debug("Generate an API Key for subscription {}", subscription);

        if (!federated && Objects.nonNull(apiKeyValue) && !apiKeyValue.isEmpty()) {
            Optional<ApiKeyEntity> existingKey = findExistingKey(apiKeyValue, subscription);
            if (existingKey.isPresent()) {
                if (canReuse(existingKey.get(), auditInfo)) {
                    return reuseExistingKey(existingKey.get(), subscription, auditInfo);
                }
                throw new ApiKeyAlreadyExistingException();
            }
        }

        ApiKeyEntity apiKey = withId.apply(generateForSubscription(subscription, apiKeyValue, federated));
        apiKeyCrudService.create(apiKey);

        // Audit
        createAuditLog(apiKey, subscription, auditInfo, ApiKeyAuditEvent.APIKEY_CREATED);

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
            return ApiKeyEntity.generateForSubscription(subscription, customApiKey);
        }

        return ApiKeyEntity.generateForSubscription(subscription);
    }

    private Optional<ApiKeyEntity> findExistingKey(String apiKeyValue, SubscriptionEntity subscription) {
        String referenceId = subscription.getReferenceId();
        String referenceType = subscription.getReferenceType() != null ? subscription.getReferenceType().name() : null;
        log.debug(
            "Look up an existing API Key for reference {} (type {}) and application {}",
            referenceId,
            referenceType,
            subscription.getApplicationId()
        );

        if (referenceId != null && referenceType != null) {
            return apiKeyQueryService.findByKeyAndReferenceIdAndReferenceType(apiKeyValue, referenceId, referenceType);
        }
        return apiKeyQueryService.findByKeyAndApiId(apiKeyValue, subscription.getApiId());
    }

    private boolean canReuse(ApiKeyEntity existingKey, AuditInfo auditInfo) {
        return isInactive(existingKey) && isCustomKeyReuseAllowed(auditInfo);
    }

    /**
     * A custom key can be reused only when its previous usage is no longer active, i.e. the key has been
     * revoked or has expired (which mirrors a closed or ended subscription). A paused key still belongs to
     * an active subscription and is therefore never reusable.
     */
    private boolean isInactive(ApiKeyEntity apiKey) {
        return apiKey.isRevoked() || apiKey.isExpired();
    }

    private boolean isCustomKeyReuseAllowed(AuditInfo auditInfo) {
        return parametersQueryService.findAsBoolean(
            Key.PLAN_SECURITY_APIKEY_CUSTOM_REUSE_ALLOWED,
            new ParameterContext(auditInfo.environmentId(), auditInfo.organizationId(), ParameterReferenceType.ENVIRONMENT)
        );
    }

    /**
     * Reuse the existing (inactive) API Key by reactivating it and attaching the new subscription, instead of
     * creating a duplicate {@code (reference, key)} record. The previous subscription link is kept for history;
     * the gateway only resolves a key through its active subscriptions, so the closed one stays inert.
     */
    private ApiKeyEntity reuseExistingKey(ApiKeyEntity existingKey, SubscriptionEntity subscription, AuditInfo auditInfo) {
        // The update writes the reactivation fields only; the link is added atomically so that links written since the key
        // was read are kept.
        apiKeyCrudService.update(existingKey.reactivate().toBuilder().expireAt(subscription.getEndingAt()).build());
        var reused = apiKeyCrudService.addSubscription(existingKey.getId(), subscription.getId()).orElseThrow(ApiKeyNotFoundException::new);

        createAuditLog(reused, subscription, auditInfo, ApiKeyAuditEvent.APIKEY_REACTIVATED);

        return reused;
    }

    private ApiKeyEntity findOrGenerate(
        AuditInfo auditInfo,
        BaseApplicationEntity application,
        SubscriptionEntity subscription,
        String customApiKey,
        boolean federated
    ) {
        return addSubscriptionToEveryKey(apiKeyQueryService.findByApplication(application.getId()), subscription).orElseGet(() ->
            generateFirstSharedKey(auditInfo, application, subscription, customApiKey, federated)
        );
    }

    /**
     * Every key of the application gets the subscription, revoked and expired ones included, so that the previous key keeps
     * working on the new subscription during the overlap that follows a shared key renewal.
     *
     * @return the key the application uses now (the non-revoked one that expires last), or empty when the application has no key
     */
    private Optional<ApiKeyEntity> addSubscriptionToEveryKey(Stream<ApiKeyEntity> keysOfTheApplication, SubscriptionEntity subscription) {
        return keysOfTheApplication
            .flatMap(apiKey -> addSubscription(apiKey, subscription).stream())
            .max(comparing(ApiKeyEntity::isRevoked, reverseOrder()).thenComparing(ApiKeyEntity::getExpireAt, nullsLast(naturalOrder())));
    }

    /**
     * The first shared key of an application has an id derived from the application, so that the database rejects all but one
     * of the first keys that requests handled at the same time by several Management API nodes try to create. A request whose
     * key was rejected adds its subscription to the key created by the other one. Later keys (renewals) keep random ids.
     */
    private ApiKeyEntity generateFirstSharedKey(
        AuditInfo auditInfo,
        BaseApplicationEntity application,
        SubscriptionEntity subscription,
        String customApiKey,
        boolean federated
    ) {
        String firstSharedKeyId = firstSharedKeyId(application.getId());
        try {
            return generate(auditInfo, subscription, customApiKey, federated, apiKey -> apiKey.toBuilder().id(firstSharedKeyId).build());
        } catch (ApiKeyIdAlreadyExistsException e) {
            log.debug("The shared API Key of application {} was created by another request meanwhile, reusing it", application.getId());
            return addSubscriptionToEveryKey(apiKeyQueryService.findByApplication(application.getId()), subscription).orElseThrow(() ->
                new TechnicalDomainException(
                    "Cannot create the shared API Key of application [" +
                        application.getId() +
                        "]: its id [" +
                        firstSharedKeyId +
                        "] is already used, but the application has no API Key",
                    e
                )
            );
        }
    }

    private static String firstSharedKeyId(String applicationId) {
        return UUID.nameUUIDFromBytes((FIRST_SHARED_KEY_ID_PREFIX + applicationId).getBytes(StandardCharsets.UTF_8)).toString();
    }

    /**
     * Attaches the subscription to the key atomically: the key read by {@link #findOrGenerate} may already be outdated (other
     * Management API nodes attach subscriptions to the same shared key concurrently), so it must not be written back.
     *
     * @return the key as stored after the change, or empty when it was deleted in the meantime
     */
    private Optional<ApiKeyEntity> addSubscription(ApiKeyEntity apiKey, SubscriptionEntity subscription) {
        return apiKeyCrudService.addSubscription(apiKey.getId(), subscription.getId());
    }

    private void createAuditLog(
        ApiKeyEntity createdApiKeyEntity,
        SubscriptionEntity subscription,
        AuditInfo auditInfo,
        ApiKeyAuditEvent event
    ) {
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
                    .event(event)
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
                    .event(event)
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
