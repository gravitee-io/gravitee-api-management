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
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.membership.domain_service.ApiProductPrimaryOwnerDomainService;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.service.common.UuidString;
import java.time.ZonedDateTime;
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

    public record Input(CreateApiProduct createApiProduct, AuditInfo auditInfo) {}

    public record Output(ApiProduct apiProduct) {}

    public Output execute(Input input) {
        var auditInfo = input.auditInfo;
        var payload = input.createApiProduct;
        var now = ZonedDateTime.now();

        ApiProduct apiProduct = new ApiProduct();
        apiProduct.setId(UuidString.generateRandom());
        apiProduct.setEnvironmentId(auditInfo.environmentId());
        apiProduct.setName(payload.getName());
        apiProduct.setDescription(payload.getDescription());
        apiProduct.setVersion(payload.getVersion());
        apiProduct.setApiIds(payload.getApiIds() == null ? Set.of() : Set.copyOf(payload.getApiIds()));
        apiProduct.setCreatedAt(now);
        apiProduct.setUpdatedAt(now);

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

        PrimaryOwnerEntity primaryOwner = PrimaryOwnerEntity.builder()
            .id(auditInfo.actor().userId())
            .email(null)
            .displayName(null)
            .type(PrimaryOwnerEntity.Type.USER)
            .build();
        apiProductPrimaryOwnerDomainService.assignPrimaryOwner(created.getId(), primaryOwner, auditInfo);

        createAuditLog(created, auditInfo);

        return new Output(created);
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
