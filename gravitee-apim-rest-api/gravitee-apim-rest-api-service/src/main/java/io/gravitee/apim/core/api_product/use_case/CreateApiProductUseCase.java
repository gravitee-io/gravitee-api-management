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
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.api_product.model.CreateApiProduct;
import io.gravitee.apim.core.api_product.query_service.ApiProductQueryService;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.ApiProductAuditLogEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.audit.model.event.ApiProductAuditEvent;
import io.gravitee.apim.core.event.crud_service.EventCrudService;
import io.gravitee.apim.core.event.crud_service.EventLatestCrudService;
import io.gravitee.apim.core.event.model.Event;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.membership.domain_service.ApiProductPrimaryOwnerDomainService;
import io.gravitee.apim.core.membership.domain_service.ApiProductPrimaryOwnerFactory;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.service.common.UuidString;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class CreateApiProductUseCase {

    private final ApiProductQueryService apiProductQueryService;
    private final ApiProductCrudService apiProductCrudService;
    private final ValidateApiProductService validateApiProductService;
    private final AuditDomainService auditService;
    private final ApiProductPrimaryOwnerDomainService apiProductPrimaryOwnerDomainService;
    private final ApiProductPrimaryOwnerFactory apiProductPrimaryOwnerFactory;
    private final EventCrudService eventCrudService;
    private final EventLatestCrudService eventLatestCrudService;

    public record Input(CreateApiProduct createApiProduct, AuditInfo auditInfo) {}

    public record Output(ApiProduct apiProduct) {}

    public Output execute(Input input) {
        var auditInfo = input.auditInfo;
        var payload = input.createApiProduct;
        var now = ZonedDateTime.now();

        List<String> apiIdsList = payload.getApiIds();
        if (apiIdsList != null && !apiIdsList.isEmpty()) {
            validateApiProductService.validateApiIdsForProduct(auditInfo.environmentId(), apiIdsList);
        }

        Set<String> apiIds = apiIdsList == null || apiIdsList.isEmpty() ? Set.of() : Set.copyOf(apiIdsList);

        ApiProduct apiProduct = ApiProduct.builder()
            .id(UuidString.generateRandom())
            .environmentId(auditInfo.environmentId())
            .name(payload.getName())
            .description(payload.getDescription())
            .version(payload.getVersion())
            .apiIds(apiIds)
            .createdAt(now)
            .updatedAt(now)
            .build();

        validateApiProductService.validate(apiProduct);

        apiProductQueryService
            .findByEnvironmentIdAndName(auditInfo.environmentId(), apiProduct.getName())
            .ifPresent(existing -> {
                throw new ValidationDomainException(
                    "An API Product already exists in environment.",
                    Map.of("environmentId", auditInfo.environmentId(), "name", apiProduct.getName())
                );
            });

        ApiProduct created = apiProductCrudService.create(apiProduct);

        PrimaryOwnerEntity primaryOwner = apiProductPrimaryOwnerFactory.createForNewApiProduct(
            auditInfo.organizationId(),
            auditInfo.environmentId(),
            auditInfo.actor().userId()
        );
        apiProductPrimaryOwnerDomainService.createApiProductPrimaryOwnerMembership(created.getId(), primaryOwner, auditInfo);

        publishDeployEvent(auditInfo, created);
        createAuditLog(created, auditInfo);

        return new Output(created);
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

    private void createAuditLog(ApiProduct apiProduct, AuditInfo auditInfo) {
        auditService.createApiProductAuditLog(
            ApiProductAuditLogEntity.builder()
                .organizationId(auditInfo.organizationId())
                .environmentId(auditInfo.environmentId())
                .event(ApiProductAuditEvent.API_PRODUCT_CREATED)
                .actor(auditInfo.actor())
                .apiProductId(apiProduct.getId())
                .newValue(apiProduct)
                .createdAt(apiProduct.getCreatedAt())
                .properties(Map.of(AuditProperties.API_PRODUCT, apiProduct.getId()))
                .build()
        );
    }
}
