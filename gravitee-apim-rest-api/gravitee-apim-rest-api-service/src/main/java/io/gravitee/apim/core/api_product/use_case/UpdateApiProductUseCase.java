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

import static java.util.Map.entry;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.api_product.crud_service.ApiProductCrudService;
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
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.service.exceptions.ForbiddenFeatureException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class UpdateApiProductUseCase {

    private final ApiProductCrudService apiProductCrudService;
    private final AuditDomainService auditService;
    private final ApiProductQueryService apiProductQueryService;
    private final ValidateApiProductService validateApiProductService;
    private final EventCrudService eventCrudService;
    private final EventLatestCrudService eventLatestCrudService;
    private final LicenseDomainService licenseDomainService;

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
            .apiIds(existingApiProduct.getApiIds() != null ? new HashSet<>(existingApiProduct.getApiIds()) : null)
            .build();

        UpdateApiProduct updateApiProduct = input.updateApiProduct();
        Set<String> apiIds = updateApiProduct.getApiIds();
        if (apiIds != null) {
            if (!apiIds.isEmpty()) {
                validateApiProductService.validateApiIdsForProduct(input.auditInfo().environmentId(), apiIds.stream().toList());
            }
            updateApiProduct.setApiIds(apiIds.isEmpty() ? Set.of() : Set.copyOf(apiIds));
        }

        existingApiProduct.update(updateApiProduct);

        ApiProduct updated = apiProductCrudService.update(existingApiProduct);
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
