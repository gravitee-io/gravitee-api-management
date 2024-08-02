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
import io.gravitee.apim.core.search.Indexer;
import io.gravitee.common.utils.TimeProvider;
import java.util.Collections;
import java.util.function.UnaryOperator;
import lombok.AllArgsConstructor;

@DomainService
@AllArgsConstructor
public class UpdateFederatedApiDomainService {

    private final ApiCrudService apiCrudService;
    private final AuditDomainService auditService;
    private final ValidateFederatedApiDomainService validateFederatedApiDomainService;
    private final CategoryDomainService categoryDomainService;
    private final ApiIndexerDomainService apiIndexerDomainService;

    public Api update(String apiId, UnaryOperator<Api> updater, AuditInfo auditInfo, PrimaryOwnerEntity primaryOwnerEntity) {
        var currentApi = apiCrudService.get(apiId);

        Api federatedApi = updater.apply(currentApi);

        var preparedApi = validateFederatedApiDomainService.validateAndSanitizeForUpdate(federatedApi, currentApi, primaryOwnerEntity);

        preparedApi.setUpdatedAt(TimeProvider.now());

        Api updated = apiCrudService.update(preparedApi);

        createAuditLog(auditInfo, preparedApi, currentApi);
        createIndex(auditInfo, preparedApi, primaryOwnerEntity);

        categoryDomainService.updateOrderCategoriesOfApi(updated.getId(), updated.getCategories());

        updated.setCategories(categoryDomainService.toCategoryKey(updated, updated.getEnvironmentId()));

        return updated;
    }

    private void createAuditLog(AuditInfo auditInfo, Api updatedApi, Api currentApi) {
        auditService.createApiAuditLog(
            ApiAuditLogEntity
                .builder()
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

    private void createIndex(AuditInfo auditInfo, Api updateApi, PrimaryOwnerEntity primaryOwnerEntity) {
        apiIndexerDomainService.index(
            new Indexer.IndexationContext(auditInfo.organizationId(), auditInfo.environmentId()),
            updateApi,
            primaryOwnerEntity
        );
    }
}
