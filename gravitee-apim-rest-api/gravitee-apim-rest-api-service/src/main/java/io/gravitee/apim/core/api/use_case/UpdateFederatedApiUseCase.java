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
package io.gravitee.apim.core.api.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.domain_service.ApiIndexerDomainService;
import io.gravitee.apim.core.api.domain_service.CategoryDomainService;
import io.gravitee.apim.core.api.domain_service.ValidateFederatedApiDomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.ApiAuditLogEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.event.ApiAuditEvent;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerDomainService;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.apim.core.search.Indexer;
import io.gravitee.common.utils.TimeProvider;
import java.util.Collections;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class UpdateFederatedApiUseCase {

    private final ApiCrudService apiCrudService;
    private final ApiPrimaryOwnerDomainService apiPrimaryOwnerDomainService;
    private final ValidateFederatedApiDomainService validateFederatedApiDomainService;
    private final AuditDomainService auditService;
    private final ApiIndexerDomainService apiIndexerDomainService;
    private final CategoryDomainService categoryDomainService;

    public Output execute(Input input) {
        var updateApi = input.apiToUpdate;
        var auditInfo = input.auditInfo;

        var currentApi = apiCrudService.get(updateApi.getId());
        PrimaryOwnerEntity primaryOwnerEntity = apiPrimaryOwnerDomainService.getApiPrimaryOwner(
            auditInfo.organizationId(),
            updateApi.getId()
        );

        var preparedApi = validateFederatedApiDomainService.validateAndSanitizeForUpdate(updateApi, currentApi, primaryOwnerEntity);

        preparedApi =
            currentApi
                .toBuilder()
                .name(preparedApi.getName())
                .description(preparedApi.getDescription())
                .version(preparedApi.getVersion())
                .apiLifecycleState(preparedApi.getApiLifecycleState())
                .visibility(preparedApi.getVisibility())
                .groups(preparedApi.getGroups())
                .labels(preparedApi.getLabels())
                .categories(categoryDomainService.toCategoryId(preparedApi, currentApi.getEnvironmentId()))
                .updatedAt(TimeProvider.now())
                .build();

        createAuditLog(auditInfo, preparedApi, currentApi);
        createIndex(auditInfo, preparedApi, primaryOwnerEntity);

        Api updated = apiCrudService.update(preparedApi);

        categoryDomainService.updateOrderCategoriesOfApi(updated.getId(), updated.getCategories());

        updated.setCategories(categoryDomainService.toCategoryKey(updated, updated.getEnvironmentId()));

        return new Output(updated, primaryOwnerEntity);
    }

    @Builder
    public record Input(Api apiToUpdate, AuditInfo auditInfo) {}

    public record Output(Api updatedApi, PrimaryOwnerEntity primaryOwnerEntity) {}

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
