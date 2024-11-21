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
import io.gravitee.apim.core.category.domain_service.UpdateCategoryApiDomainService;
import io.gravitee.apim.core.flow.crud_service.FlowCrudService;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.apim.core.notification.domain_service.TriggerNotificationDomainService;
import io.gravitee.apim.core.notification.model.hook.ApiDeprecatedApiHookContext;
import io.gravitee.apim.core.notification.model.hook.ApiUpdatedApiHookContext;
import io.gravitee.apim.core.plan.domain_service.DeprecatePlanDomainService;
import io.gravitee.apim.core.plan.query_service.PlanQueryService;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import java.util.Collections;
import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@DomainService
public class UpdateNativeApiDomainService {

    private final ApiCrudService apiCrudService;
    private final PlanQueryService planQueryService;
    private final DeprecatePlanDomainService deprecatePlanDomainService;
    private final TriggerNotificationDomainService triggerNotificationDomainService;
    private final FlowCrudService flowCrudService;
    private final CategoryDomainService categoryDomainService;
    private final AuditDomainService auditService;
    private final ApiIndexerDomainService apiIndexerDomainService;

    public Api update(
        String apiId,
        UnaryOperator<Api> updater,
        BinaryOperator<Api> sanitizer,
        AuditInfo auditInfo,
        PrimaryOwnerEntity primaryOwnerEntity,
        ApiIndexerDomainService.Context ctx
    ) {
        var existingApi = apiCrudService.get(apiId);

        Api apiWithUpdatedFields = updater.apply(existingApi);

        var api = sanitizer.apply(existingApi, apiWithUpdatedFields);
        api.setUpdatedAt(TimeProvider.now());

        if (Api.ApiLifecycleState.DEPRECATED == api.getApiLifecycleState()) {
            handleDeprecatedApi(api, auditInfo);
        }

        var updatedApi = apiCrudService.update(api);

        // update api flows
        flowCrudService.saveNativeApiFlows(updatedApi.getId(), updatedApi.getApiDefinitionNativeV4().getFlows());

        // update api categories
        categoryDomainService.updateOrderCategoriesOfApi(updatedApi.getId(), updatedApi.getCategories());

        // audit service create api audit log
        createAuditLog(auditInfo, updatedApi, existingApi);

        // Notification of update
        triggerNotificationDomainService.triggerApiNotification(
            auditInfo.organizationId(),
            auditInfo.environmentId(),
            new ApiUpdatedApiHookContext(api.getId())
        );

        updatedApi.setCategories(categoryDomainService.toCategoryKey(updatedApi, auditInfo.environmentId()));

        // index in search engine
        apiIndexerDomainService.index(ctx, updatedApi, primaryOwnerEntity);

        return updatedApi;
    }

    private void handleDeprecatedApi(Api api, AuditInfo auditInfo) {
        planQueryService
            .findAllByApiId(api.getId())
            .stream()
            .filter(plan -> PlanStatus.PUBLISHED == plan.getPlanStatus() || PlanStatus.STAGING == plan.getPlanStatus())
            .forEach(plan -> deprecatePlanDomainService.deprecate(plan.getId(), auditInfo, true));

        triggerNotificationDomainService.triggerApiNotification(
            auditInfo.organizationId(),
            auditInfo.environmentId(),
            new ApiDeprecatedApiHookContext(api.getId())
        );
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
}
