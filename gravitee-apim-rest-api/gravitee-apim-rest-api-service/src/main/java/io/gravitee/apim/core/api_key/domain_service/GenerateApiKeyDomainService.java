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
import io.gravitee.apim.core.api_key.model.ApiKeyEntity;
import io.gravitee.apim.core.api_key.query_service.ApiKeyQueryService;
import io.gravitee.apim.core.application.crud_service.ApplicationCrudService;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.ApiAuditLogEntity;
import io.gravitee.apim.core.audit.model.ApiProductAuditLogEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.audit.model.event.ApiKeyAuditEvent;
import io.gravitee.apim.core.parameters.model.ParameterContext;
import io.gravitee.apim.core.parameters.query_service.ParametersQueryService;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.apim.core.subscription.model.SubscriptionReferenceType;
import io.gravitee.repository.management.model.ApiKey;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.service.exceptions.ApiKeyAlreadyExistingException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

@DomainService
@CustomLog
@RequiredArgsConstructor
public class GenerateApiKeyDomainService {

    private final ApiKeyCrudService apiKeyCrudService;
    private final ApiKeyQueryService apiKeyQueryService;
    private final ApplicationCrudService applicationCrudService;
    private final AuditDomainService auditService;
    private final CustomApiKeyAvailabilityDomainService customApiKeyAvailabilityDomainService;
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
        log.debug("Generate an API Key for subscription {}", subscription);

        if (Objects.nonNull(apiKeyValue) && !apiKeyValue.isEmpty() && !federated) {
            var reused = generateOrReuseCustomKey(subscription, apiKeyValue, auditInfo);
            if (reused.isPresent()) {
                return reused.get();
            }
        }

        ApiKeyEntity apiKey = generateForSubscription(subscription, apiKeyValue, federated);
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

    private boolean isCustomKeyAvailable(SubscriptionEntity subscription, String customApiKey) {
        return customApiKeyAvailabilityDomainService.canUseCustomKey(
            customApiKey,
            subscription.getReferenceId() != null ? subscription.getReferenceId() : subscription.getApiId(),
            subscription.getReferenceType() != null ? subscription.getReferenceType().name() : SubscriptionReferenceType.API.name(),
            subscription.getApplicationId(),
            subscription.getEnvironmentId()
        );
    }

    private Optional<ApiKeyEntity> generateOrReuseCustomKey(SubscriptionEntity subscription, String customApiKey, AuditInfo auditInfo) {
        if (!isCustomKeyAvailable(subscription, customApiKey)) {
            throw new ApiKeyAlreadyExistingException();
        }
        if (!isCustomKeyReuseAllowed(auditInfo)) {
            return Optional.empty();
        }
        return reuseExistingCustomKey(subscription, customApiKey, auditInfo);
    }

    private boolean isCustomKeyReuseAllowed(AuditInfo auditInfo) {
        return parametersQueryService.findAsBoolean(
            Key.PLAN_SECURITY_APIKEY_CUSTOM_REUSE_ALLOWED,
            new ParameterContext(auditInfo.environmentId(), auditInfo.organizationId(), ParameterReferenceType.ENVIRONMENT)
        );
    }

    private Optional<ApiKeyEntity> reuseExistingCustomKey(SubscriptionEntity subscription, String customApiKey, AuditInfo auditInfo) {
        return apiKeyQueryService
            .findByKeyAndEnvironmentId(customApiKey, subscription.getEnvironmentId())
            .stream()
            .filter(apiKey -> subscription.getApplicationId().equals(apiKey.getApplicationId()))
            .max(Comparator.comparing(ApiKeyEntity::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
            .map(existing -> reactivateAndUpdateExistingKey(existing, subscription, auditInfo));
    }

    private ApiKeyEntity reactivateAndUpdateExistingKey(ApiKeyEntity existing, SubscriptionEntity subscription, AuditInfo auditInfo) {
        var updated = existing.reactivate().addSubscription(subscription.getId());
        if (subscription.getEndingAt() != null) {
            updated = updated.toBuilder().expireAt(subscription.getEndingAt()).build();
        }
        apiKeyCrudService.update(updated);
        createAuditLog(updated, subscription, auditInfo, ApiKeyAuditEvent.APIKEY_REACTIVATED);
        return updated;
    }

    private ApiKeyEntity findOrGenerate(
        AuditInfo auditInfo,
        BaseApplicationEntity application,
        SubscriptionEntity subscription,
        String customApiKey,
        boolean federated
    ) {
        return apiKeyQueryService
            .findByApplication(application.getId())
            .peek(apiKey -> addSubscription(apiKey, subscription))
            .max(comparing(ApiKeyEntity::isRevoked, reverseOrder()).thenComparing(ApiKeyEntity::getExpireAt, nullsLast(naturalOrder())))
            .orElseGet(() -> generate(auditInfo, subscription, customApiKey, federated));
    }

    private void addSubscription(ApiKeyEntity apiKey, SubscriptionEntity subscription) {
        apiKeyCrudService.update(apiKey.addSubscription(subscription.getId()));
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
        properties.put(AuditProperties.API_KEY, createdApiKeyEntity.getKey());
        if (referenceId != null) {
            properties.put(isApiProduct ? AuditProperties.API_PRODUCT : AuditProperties.API, referenceId);
        }
        properties.put(AuditProperties.APPLICATION, subscription.getApplicationId());

        var auditTimestamp = event == ApiKeyAuditEvent.APIKEY_REACTIVATED
            ? createdApiKeyEntity.getUpdatedAt()
            : createdApiKeyEntity.getCreatedAt();

        if (isApiProduct) {
            auditService.createApiProductAuditLog(
                ApiProductAuditLogEntity.builder()
                    .organizationId(auditInfo.organizationId())
                    .environmentId(auditInfo.environmentId())
                    .apiProductId(referenceId)
                    .event(event)
                    .actor(auditInfo.actor())
                    .oldValue(null)
                    .newValue(createdApiKeyEntity)
                    .createdAt(auditTimestamp)
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
                    .newValue(createdApiKeyEntity)
                    .createdAt(auditTimestamp)
                    .properties(properties)
                    .build()
            );
        }
    }
}
