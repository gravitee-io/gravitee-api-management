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
package io.gravitee.apim.core.api.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.ApiAuditLogEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.event.ApiAuditEvent;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.common.utils.TimeProvider;
import java.util.Collections;
import java.util.function.UnaryOperator;
import lombok.AllArgsConstructor;

/**
 * Updates a standalone agent API. Sibling of {@link UpdateFederatedApiDomainService}: the caller-supplied updater
 * carries the new management fields and the fresh {@code AgentApi} definition, this service persists, audits and
 * re-indexes it. Agents stay persisted as {@code definitionVersion=V4 + type=AGENT}.
 */
@DomainService
@AllArgsConstructor
public class UpdateAgentApiDomainService {

    private final ApiCrudService apiCrudService;
    private final AuditDomainService auditService;
    private final ApiIndexerDomainService apiIndexerDomainService;

    public Api update(
        String apiId,
        UnaryOperator<Api> updater,
        AuditInfo auditInfo,
        PrimaryOwnerEntity primaryOwnerEntity,
        ApiIndexerDomainService.Context ctx
    ) {
        var currentApi = apiCrudService.get(apiId);

        Api agentApi = updater.apply(currentApi);
        agentApi.setUpdatedAt(TimeProvider.now());

        Api updated = apiCrudService.update(agentApi);

        createAuditLog(auditInfo, updated, currentApi);
        apiIndexerDomainService.index(ctx, updated, primaryOwnerEntity);

        return updated;
    }

    private void createAuditLog(AuditInfo auditInfo, Api updatedApi, Api currentApi) {
        auditService.createApiAuditLog(
            ApiAuditLogEntity.builder()
                .organizationId(auditInfo.organizationId())
                .environmentId(auditInfo.environmentId())
                .apiId(updatedApi.getId())
                .event(ApiAuditEvent.API_UPDATED)
                .actor(auditInfo.actor())
                .oldValue(currentApi)
                .newValue(updatedApi)
                .createdAt(updatedApi.getUpdatedAt())
                .properties(Collections.emptyMap())
                .build()
        );
    }
}
