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

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.api_key.crud_service.ApiKeyCrudService;
import io.gravitee.apim.core.api_key.model.ApiKeyEntity;
import io.gravitee.apim.core.api_key.query_service.ApiKeyQueryService;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.ApiAuditLogEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.audit.model.event.ApiKeyAuditEvent;
import io.gravitee.apim.core.notification.domain_service.TriggerNotificationDomainService;
import io.gravitee.apim.core.notification.model.hook.ApiKeyRevokedApiHookContext;
import io.gravitee.apim.core.subscription.crud_service.SubscriptionCrudService;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class RevokeApiKeyDomainService {

    private final ApiKeyCrudService apiKeyCrudService;
    private final ApiKeyQueryService apiKeyQueryService;
    private final SubscriptionCrudService subscriptionCrudService;
    private final AuditDomainService auditService;
    private final TriggerNotificationDomainService triggerNotificationDomainService;

    public Set<ApiKeyEntity> revokeAllSubscriptionsApiKeys(SubscriptionEntity subscription, AuditInfo auditInfo) {
        return apiKeyQueryService
            .findBySubscription(subscription.getId())
            .filter(ApiKeyEntity::canBeRevoked)
            .map(apiKeyEntity -> {
                var revokedApiKey = apiKeyEntity.revoke();
                apiKeyCrudService.update(revokedApiKey);

                createAuditLog(apiKeyEntity, revokedApiKey, subscription, auditInfo);
                return revokedApiKey;
            })
            .collect(Collectors.toSet());
    }

    public ApiKeyEntity revoke(ApiKeyEntity apiKey, AuditInfo auditInfo) {
        if (!apiKey.canBeRevoked()) {
            return apiKey;
        }

        var revoked = apiKeyCrudService.update(apiKey.revoke());

        apiKey
            .getSubscriptions()
            .forEach(subscriptionId -> {
                var subscription = subscriptionCrudService.get(subscriptionId);
                createAuditLog(apiKey, revoked, subscription, auditInfo);
                triggerNotificationDomainService.triggerApiNotification(
                    auditInfo.organizationId(),
                    auditInfo.environmentId(),
                    new ApiKeyRevokedApiHookContext(
                        subscription.getApiId(),
                        subscription.getApplicationId(),
                        subscription.getPlanId(),
                        apiKey.getKey()
                    )
                );
            });

        return revoked;
    }

    private void createAuditLog(
        ApiKeyEntity apiKeyEntity,
        ApiKeyEntity revokedApiKeyEntity,
        SubscriptionEntity subscription,
        AuditInfo auditInfo
    ) {
        auditService.createApiAuditLog(
            ApiAuditLogEntity
                .builder()
                .organizationId(auditInfo.organizationId())
                .environmentId(auditInfo.environmentId())
                .apiId(subscription.getApiId())
                .event(ApiKeyAuditEvent.APIKEY_REVOKED)
                .actor(auditInfo.actor())
                .oldValue(apiKeyEntity)
                .newValue(revokedApiKeyEntity)
                .createdAt(ZonedDateTime.ofInstant(revokedApiKeyEntity.getRevokedAt().toInstant(), ZoneId.systemDefault()))
                .properties(
                    Map.of(
                        AuditProperties.API_KEY,
                        apiKeyEntity.getKey(),
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
