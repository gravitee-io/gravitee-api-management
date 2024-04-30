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
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.audit.model.event.ApiKeyAuditEvent;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.repository.management.model.ApiKey;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.gravitee.rest.api.service.exceptions.ApiKeyAlreadyExistingException;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

@DomainService
@Slf4j
public class GenerateApiKeyDomainService {

    private final ApiKeyCrudService apiKeyCrudService;
    private final ApiKeyQueryService apiKeyQueryService;
    private final ApplicationCrudService applicationCrudService;
    private final AuditDomainService auditService;

    public GenerateApiKeyDomainService(
        ApiKeyCrudService apiKeyCrudService,
        ApiKeyQueryService apiKeyQueryService,
        ApplicationCrudService applicationCrudService,
        AuditDomainService auditService
    ) {
        this.apiKeyCrudService = apiKeyCrudService;
        this.apiKeyQueryService = apiKeyQueryService;
        this.applicationCrudService = applicationCrudService;
        this.auditService = auditService;
    }

    public ApiKeyEntity generate(SubscriptionEntity subscription, AuditInfo auditInfo, String customApiKey) {
        var app = applicationCrudService.findById(subscription.getApplicationId(), auditInfo.environmentId());
        return generate(subscription, app, auditInfo, customApiKey);
    }

    public ApiKeyEntity generate(
        SubscriptionEntity subscription,
        BaseApplicationEntity application,
        AuditInfo auditInfo,
        String customApiKey
    ) {
        if (!application.hasApiKeySharedMode()) {
            return generate(auditInfo, subscription, customApiKey);
        }
        return findOrGenerate(auditInfo, application, subscription, customApiKey);
    }

    private ApiKeyEntity generate(AuditInfo auditInfo, SubscriptionEntity subscription, String customApiKey) {
        log.debug("Generate an API Key for subscription {}", subscription);

        ApiKeyEntity apiKey = generateForSubscription(subscription, customApiKey);
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
     * @return An API Key
     */
    private ApiKeyEntity generateForSubscription(SubscriptionEntity subscription, String customApiKey) {
        if (Objects.nonNull(customApiKey) && !customApiKey.isEmpty()) {
            if (!isKeyExistFor(customApiKey, subscription)) {
                throw new ApiKeyAlreadyExistingException();
            }
            return ApiKeyEntity.generateForSubscription(subscription, customApiKey, false);
        }

        return ApiKeyEntity.generateForSubscription(subscription);
    }

    private boolean isKeyExistFor(String apiKeyValue, SubscriptionEntity subscription) {
        String apiId = subscription.getApiId();
        log.debug("Check if an API Key can be created for api {} and application {}", apiId, subscription.getApplicationId());

        return apiKeyQueryService.findByKeyAndApiId(apiKeyValue, apiId).isEmpty();
    }

    private ApiKeyEntity findOrGenerate(
        AuditInfo auditInfo,
        BaseApplicationEntity application,
        SubscriptionEntity subscription,
        String customApiKey
    ) {
        return apiKeyQueryService
            .findByApplication(application.getId())
            .peek(apiKey -> addSubscription(apiKey, subscription))
            .max(comparing(ApiKeyEntity::isRevoked, reverseOrder()).thenComparing(ApiKeyEntity::getExpireAt, nullsLast(naturalOrder())))
            .orElseGet(() -> generate(auditInfo, subscription, customApiKey));
    }

    private void addSubscription(ApiKeyEntity apiKey, SubscriptionEntity subscription) {
        apiKeyCrudService.update(apiKey.addSubscription(subscription.getId()));
    }

    private void createAuditLog(ApiKeyEntity createdApiKeyEntity, SubscriptionEntity subscription, AuditInfo auditInfo) {
        auditService.createApiAuditLog(
            ApiAuditLogEntity
                .builder()
                .organizationId(auditInfo.organizationId())
                .environmentId(auditInfo.environmentId())
                .apiId(subscription.getApiId())
                .event(ApiKeyAuditEvent.APIKEY_CREATED)
                .actor(auditInfo.actor())
                .oldValue(null)
                .newValue(createdApiKeyEntity)
                .createdAt(createdApiKeyEntity.getCreatedAt())
                .properties(
                    Map.of(
                        AuditProperties.API_KEY,
                        createdApiKeyEntity.getKey(),
                        AuditProperties.API,
                        subscription.getApiId(),
                        AuditProperties.APPLICATION,
                        subscription.getApplicationId()
                    )
                )
                .build()
        );
    }
}
