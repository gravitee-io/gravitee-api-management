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
import static java.util.Map.entry;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.domain_service.ApiIndexerDomainService;
import io.gravitee.apim.core.api.domain_service.ApiStateDomainService;
import io.gravitee.apim.core.api.model.Api;
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
import io.gravitee.apim.core.event.crud_service.EventCrudService;
import io.gravitee.apim.core.event.crud_service.EventLatestCrudService;
import io.gravitee.apim.core.event.model.Event;
import io.gravitee.apim.core.license.domain_service.LicenseDomainService;
import io.gravitee.apim.core.membership.domain_service.ApiProductPrimaryOwnerDomainService;
import io.gravitee.apim.core.membership.exception.ApiProductPrimaryOwnerNotFoundException;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.service.exceptions.ForbiddenFeatureException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
@CustomLog
public class UpdateApiProductUseCase {

    private final ApiProductCrudService apiProductCrudService;
    private final ApiCrudService apiCrudService;
    private final ApiIndexerDomainService apiIndexerDomainService;
    private final AuditDomainService auditService;
    private final ApiProductQueryService apiProductQueryService;
    private final ValidateApiProductService validateApiProductService;
    private final ApiStateDomainService apiStateDomainService;
    private final EventCrudService eventCrudService;
    private final EventLatestCrudService eventLatestCrudService;
    private final LicenseDomainService licenseDomainService;
    private final ApiProductIndexerDomainService apiProductIndexerDomainService;
    private final ApiProductPrimaryOwnerDomainService apiProductPrimaryOwnerDomainService;

    public Output execute(Input input) {
        if (!licenseDomainService.isApiProductDeploymentAllowed(input.auditInfo().organizationId())) {
            throw new ForbiddenFeatureException("api-product");
        }
        Optional<ApiProduct> existingApiProductOpt = apiProductQueryService.findById(input.apiProductId());
        if (existingApiProductOpt.isEmpty()) {
            throw new ApiProductNotFoundException(input.apiProductId());
        }
        ApiProduct existingApiProduct = existingApiProductOpt.get();

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

        existingApiProduct.update(updateApiProduct);

        ApiProduct updated = apiProductCrudService.update(existingApiProduct);

        // Re-index APIs whose product membership changed so Lucene api_product_ids stays in sync.
        // so if i add api-id "467318654398789dgf37" to api-product, we re-index that 1 API (and the product).
        // Removed APIs: api_product_ids must drop this product. Added APIs: api_product_ids must include it.
        Set<String> beforeIds = beforeUpdate.getApiIds() != null ? beforeUpdate.getApiIds() : Set.of();
        Set<String> afterIds = updated.getApiIds() != null ? updated.getApiIds() : Set.of();
        Set<String> removed = new HashSet<>(beforeIds);
        removed.removeAll(afterIds);
        Set<String> added = new HashSet<>(afterIds);
        added.removeAll(beforeIds);

        List<String> apiIdsToReindex = Stream.concat(removed.stream(), added.stream()).toList(); // get the list of API IDs to re-index. union of removed and added (only IDs that changed).
        List<Api> apisToReindex = apiIdsToReindex // load the full Api entity for each of those IDs (so we can re-index them).
            .stream()
            .flatMap(apiId -> apiCrudService.findById(apiId).stream())
            .toList();
        var indexation = ApiIndexerDomainService.oneShotIndexation(input.auditInfo());
        for (Api api : apisToReindex) {
            apiIndexerDomainService.index(indexation, api);
        }

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

        publishDeployEvent(input.auditInfo(), updated);
        createAuditLog(beforeUpdate, updated, input.auditInfo());
        return new Output(updated);
    }

    private void publishDeployEvent(AuditInfo auditInfo, ApiProduct apiProduct) {
        final Event event = eventCrudService.createEvent(
            auditInfo.organizationId(),
            auditInfo.environmentId(),
            Set.of(auditInfo.environmentId()),
            EventType.DEPLOY_API_PRODUCT,
            apiProduct,
            Map.ofEntries(
                entry(Event.EventProperties.USER, auditInfo.actor().userId()),
                entry(Event.EventProperties.API_PRODUCT_ID, apiProduct.getId())
            )
        );

        eventLatestCrudService.createOrPatchLatestEvent(auditInfo.organizationId(), apiProduct.getId(), event);
    }

    public record Input(String apiProductId, UpdateApiProduct updateApiProduct, AuditInfo auditInfo) {
        public static Input of(String apiProductId, UpdateApiProduct updateApiProduct, AuditInfo auditInfo) {
            return new Input(apiProductId, updateApiProduct, auditInfo);
        }
    }

    public record Output(ApiProduct apiProduct) {}

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
