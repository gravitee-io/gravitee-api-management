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

import io.gravitee.apim.core.api_key.crud_service.ApiKeyCrudService;
import io.gravitee.apim.core.api_key.model.ApiKeyEntity;
import io.gravitee.apim.core.api_key.query_service.ApiKeyQueryService;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.ApiAuditLogEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.audit.model.event.ApiKeyAuditEvent;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class RevokeApiKeyDomainService {

    private final ApiKeyCrudService apiKeyCrudService;
    private final ApiKeyQueryService apiKeyQueryService;
    private final AuditDomainService auditService;

    public RevokeApiKeyDomainService(
        ApiKeyCrudService apiKeyCrudService,
        ApiKeyQueryService apiKeyQueryService,
        AuditDomainService auditService
    ) {
        this.apiKeyCrudService = apiKeyCrudService;
        this.apiKeyQueryService = apiKeyQueryService;
        this.auditService = auditService;
    }

    public Set<ApiKeyEntity> revokeAllSubscriptionsApiKeys(String apiId, String subscriptionId, AuditInfo auditInfo) {
        return apiKeyQueryService
            .findBySubscription(subscriptionId)
            .filter(ApiKeyEntity::canBeRevoked)
            .map(apiKeyEntity -> {
                var revokedApiKey = apiKeyEntity.revoke();
                apiKeyCrudService.update(revokedApiKey);

                createAuditLog(apiId, apiKeyEntity, revokedApiKey, auditInfo);
                return revokedApiKey;
            })
            .collect(Collectors.toSet());
    }

    private void createAuditLog(String apiId, ApiKeyEntity apiKeyEntity, ApiKeyEntity revokedApiKeyEntity, AuditInfo auditInfo) {
        auditService.createApiAuditLog(
            ApiAuditLogEntity
                .builder()
                .organizationId(auditInfo.organizationId())
                .environmentId(auditInfo.environmentId())
                .apiId(apiId)
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
                        apiId,
                        AuditProperties.APPLICATION,
                        apiKeyEntity.getApplicationId()
                    )
                )
                .build()
        );
    }
}
