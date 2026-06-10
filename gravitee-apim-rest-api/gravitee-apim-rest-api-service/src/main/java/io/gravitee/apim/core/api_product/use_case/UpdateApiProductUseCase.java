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
package io.gravitee.apim.core.api_product.use_case;

import static io.gravitee.apim.core.api_product.domain_service.ApiProductIndexerDomainService.oneShotIndexation;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.api.domain_service.ApiStateDomainService;
import io.gravitee.apim.core.api_product.crud_service.ApiProductCrudService;
import io.gravitee.apim.core.api_product.domain_service.ApiProductIndexerDomainService;
import io.gravitee.apim.core.api_product.domain_service.ValidateApiProductService;
import io.gravitee.apim.core.api_product.exception.ApiProductNotFoundException;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.api_product.model.UpdateApiProduct;
import io.gravitee.apim.core.api_product.query_service.ApiProductQueryService;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.ApiProductAuditLogEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.audit.model.event.ApiProductAuditEvent;
import io.gravitee.apim.core.license.domain_service.LicenseDomainService;
import io.gravitee.apim.core.membership.domain_service.ApiProductPrimaryOwnerDomainService;
import io.gravitee.apim.core.membership.exception.ApiProductPrimaryOwnerNotFoundException;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.apim.core.notification.domain_service.TriggerNotificationDomainService;
import io.gravitee.apim.core.notification.model.hook.ApiProductUpdatedHookContext;
import io.gravitee.apim.core.plan.crud_service.PlanCrudService;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.plan.query_service.PlanQueryService;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.service.exceptions.ForbiddenFeatureException;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
@CustomLog
public class UpdateApiProductUseCase {

    private final ApiProductCrudService apiProductCrudService;
    private final AuditDomainService auditService;
    private final ApiProductQueryService apiProductQueryService;
    private final ValidateApiProductService validateApiProductService;
    private final ApiStateDomainService apiStateDomainService;
    private final LicenseDomainService licenseDomainService;
    private final ApiProductIndexerDomainService apiProductIndexerDomainService;
    private final ApiProductPrimaryOwnerDomainService apiProductPrimaryOwnerDomainService;
    private final TriggerNotificationDomainService triggerNotificationDomainService;
    private final PlanQueryService planQueryService;
    private final PlanCrudService planCrudService;

    public Output execute(Input input) {
        if (!licenseDomainService.isApiProductDeploymentAllowed(input.auditInfo().organizationId())) {
            throw new ForbiddenFeatureException("api-product");
        }
        Optional<ApiProduct> existingApiProductOpt = apiProductQueryService.findById(input.apiProductId());
        if (existingApiProductOpt.isEmpty()) {
            throw new ApiProductNotFoundException(input.apiProductId());
        }
        ApiProduct existingApiProduct = existingApiProductOpt.get();
        if (!input.auditInfo().environmentId().equals(existingApiProduct.getEnvironmentId())) {
            throw new ApiProductNotFoundException(input.apiProductId());
        }

        ApiProduct beforeUpdate = existingApiProduct
            .toBuilder()
            .apiIds(existingApiProduct.getApiIds() != null ? new java.util.HashSet<>(existingApiProduct.getApiIds()) : null)
            .build();

        UpdateApiProduct updateApiProduct = input.updateApiProduct();
        Set<String> apiIds = updateApiProduct.getApiIds();
        if (apiIds != null) {
            Set<String> newApiIds = apiIds.isEmpty() ? Set.of() : Set.copyOf(apiIds);
            if (!apiIds.isEmpty()) {
                validateApiProductService.validateApiIdsForProduct(input.auditInfo().environmentId(), apiIds.stream().toList());
            }
            updateApiProduct.setApiIds(newApiIds);

            Set<String> beforeIds = beforeUpdate.getApiIds() != null ? beforeUpdate.getApiIds() : Set.of();
            Set<String> removedApiIds = new HashSet<>(beforeIds);
            removedApiIds.removeAll(newApiIds);
            if (!removedApiIds.isEmpty()) {
                for (var api : validateApiProductService.getApisToUndeployOnRemoval(removedApiIds, input.apiProductId())) {
                    apiStateDomainService.stop(api, input.auditInfo());
                }
            }
        }

        if (updateApiProduct.getTags() != null) {
            existingApiProduct.setTags(updateApiProduct.getTags().isEmpty() ? Set.of() : Set.copyOf(updateApiProduct.getTags()));
        }

        existingApiProduct.update(updateApiProduct);

        ApiProduct updated = apiProductCrudService.update(existingApiProduct);

        autoCleanOrphanedPlanTags(updated, beforeUpdate.getTags());

        PrimaryOwnerEntity primaryOwner = null;
        try {
            primaryOwner = apiProductPrimaryOwnerDomainService.getApiProductPrimaryOwner(
                input.auditInfo().organizationId(),
                updated.getId()
            );
        } catch (ApiProductPrimaryOwnerNotFoundException ex) {
            log.debug("Failed to retrieve API Product primary owner, will index without primary owner", ex);
        }
        apiProductIndexerDomainService.index(oneShotIndexation(input.auditInfo()), updated, primaryOwner);

        createAuditLog(beforeUpdate, updated, input.auditInfo());
        // Notification reflects the persisted record change; gateway deploy is explicit via DeployApiProductUseCase.
        triggerNotificationDomainService.triggerApiProductNotification(
            input.auditInfo().organizationId(),
            input.auditInfo().environmentId(),
            new ApiProductUpdatedHookContext(updated.getId(), input.auditInfo().actor().userId())
        );
        return new Output(updated);
    }

    public record Input(String apiProductId, UpdateApiProduct updateApiProduct, AuditInfo auditInfo) {
        public static Input of(String apiProductId, UpdateApiProduct updateApiProduct, AuditInfo auditInfo) {
            return new Input(apiProductId, updateApiProduct, auditInfo);
        }
    }

    public record Output(ApiProduct apiProduct) {}

    /**
     * When tags are removed from the product, strip plan tags that are no longer present
     * in the product's tag set. Only triggers when at least one previously-existing tag
     * was removed (i.e. the effective set shrank or shifted).
     */
    private void autoCleanOrphanedPlanTags(ApiProduct updatedProduct, Set<String> previousProductTags) {
        Set<String> currentProductTags = updatedProduct.getTags();
        if (!hasRemovedTags(previousProductTags, currentProductTags)) {
            return;
        }

        planQueryService
            .findAllByReferenceIdAndReferenceType(updatedProduct.getId(), GenericPlanEntity.ReferenceType.API_PRODUCT)
            .forEach(plan -> removeOrphanedTagsFromPlan(plan, updatedProduct.getId(), currentProductTags));
    }

    private static boolean hasRemovedTags(Set<String> previousTags, Set<String> currentTags) {
        if (previousTags == null || previousTags.isEmpty()) {
            return false;
        }
        if (currentTags == null || currentTags.isEmpty()) {
            return true;
        }
        return !currentTags.containsAll(previousTags);
    }

    private void removeOrphanedTagsFromPlan(Plan plan, String productId, Set<String> currentProductTags) {
        var planDef = plan.getPlanDefinitionHttpV4();
        if (planDef == null || planDef.getTags() == null || planDef.getTags().isEmpty()) {
            return;
        }

        Set<String> cleanedTags = (currentProductTags == null || currentProductTags.isEmpty())
            ? Set.of()
            : planDef.getTags().stream().filter(currentProductTags::contains).collect(Collectors.toSet());
        if (cleanedTags.size() >= planDef.getTags().size()) {
            return;
        }

        log.info(
            "Auto-cleaning orphaned tags from plan [{}] of API Product [{}]: {} -> {}",
            plan.getId(),
            productId,
            planDef.getTags(),
            cleanedTags
        );
        planDef.setTags(cleanedTags.isEmpty() ? null : cleanedTags);
        planCrudService.update(plan);
    }

    private void createAuditLog(ApiProduct before, ApiProduct after, AuditInfo auditInfo) {
        auditService.createApiProductAuditLog(
            ApiProductAuditLogEntity.builder()
                .organizationId(auditInfo.organizationId())
                .environmentId(auditInfo.environmentId())
                .event(ApiProductAuditEvent.API_PRODUCT_UPDATED)
                .apiProductId(after.getId())
                .actor(auditInfo.actor())
                .oldValue(before)
                .newValue(after)
                .createdAt(TimeProvider.now())
                .properties(Map.of(AuditProperties.API_PRODUCT, after.getId()))
                .build()
        );
    }
}
