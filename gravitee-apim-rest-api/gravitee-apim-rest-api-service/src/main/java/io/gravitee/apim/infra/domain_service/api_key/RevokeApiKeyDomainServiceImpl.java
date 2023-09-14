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
package io.gravitee.apim.infra.domain_service.api_key;

import io.gravitee.apim.core.api_key.domain_service.RevokeApiKeyDomainService;
import io.gravitee.apim.core.api_key.model.ApiKeyEntity;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.ApiAuditLogEntity;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.audit.model.event.ApiKeyAuditEvent;
import io.gravitee.apim.infra.adapter.ApiKeyAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RevokeApiKeyDomainServiceImpl implements RevokeApiKeyDomainService {

    private final ApiKeyRepository apiKeyRepository;
    private final AuditDomainService auditService;

    public RevokeApiKeyDomainServiceImpl(@Lazy ApiKeyRepository apiKeyRepository, AuditDomainService auditService) {
        this.apiKeyRepository = apiKeyRepository;
        this.auditService = auditService;
    }

    @Override
    public Set<ApiKeyEntity> revokeAllSubscriptionsApiKeys(
        ExecutionContext executionContext,
        String apiId,
        String subscriptionId,
        AuditActor currentUser
    ) {
        try {
            return apiKeyRepository
                .findBySubscription(subscriptionId)
                .stream()
                .map(ApiKeyAdapter.INSTANCE::toEntity)
                .filter(ApiKeyEntity::canBeRevoked)
                .map(apiKeyEntity -> {
                    var revokedApiKey = apiKeyEntity.revoke();
                    var convertedApiKey = ApiKeyAdapter.INSTANCE.fromEntity(apiKeyEntity);
                    try {
                        apiKeyRepository.update(convertedApiKey);

                        createAuditLog(executionContext, apiId, currentUser, apiKeyEntity, revokedApiKey);
                        return revokedApiKey;
                    } catch (TechnicalException e) {
                        throw new TechnicalManagementException(e);
                    }
                })
                .collect(Collectors.toSet());
        } catch (TechnicalException e) {
            throw new TechnicalManagementException(e);
        }
    }

    private void createAuditLog(
        ExecutionContext executionContext,
        String apiId,
        AuditActor currentUser,
        ApiKeyEntity apiKeyEntity,
        ApiKeyEntity revokedApiKeyEntity
    ) {
        auditService.createApiAuditLog(
            ApiAuditLogEntity
                .builder()
                .organizationId(executionContext.getOrganizationId())
                .environmentId(executionContext.getEnvironmentId())
                .apiId(apiId)
                .event(ApiKeyAuditEvent.APIKEY_REVOKED)
                .actor(currentUser)
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
                        apiKeyEntity.getApplication()
                    )
                )
                .build()
        );
    }
}
