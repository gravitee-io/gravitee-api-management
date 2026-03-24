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
package io.gravitee.apim.core.api_product.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.api_product.crud_service.ApiProductCrudService;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.api_product.query_service.ApiProductQueryService;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.ApiProductAuditLogEntity;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.audit.model.event.ApiProductAuditEvent;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.common.utils.TimeProvider;
import java.util.Map;
import java.util.Set;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
@CustomLog
public class RemoveApiFromApiProductsDomainService {

    private final ApiProductQueryService apiProductQueryService;
    private final ApiProductCrudService apiProductCrudService;
    private final ValidateApiProductService validateApiProductService;
    private final DeployApiProductDomainService deployApiProductDomainService;
    private final AuditDomainService auditDomainService;

    public void removeApiFromApiProducts(String apiId, String organizationId, String environmentId, String userId) {
        Set<ApiProduct> affectedProducts = apiProductQueryService.findByApiId(apiId);
        if (affectedProducts.isEmpty()) {
            return;
        }

        AuditInfo auditInfo = AuditInfo.builder()
            .organizationId(organizationId)
            .environmentId(environmentId)
            .actor(AuditActor.builder().userId(userId).build())
            .build();

        affectedProducts.forEach(apiProduct -> apiProduct.removeApiId(apiId));

        apiProductCrudService.removeApiFromAllApiProducts(apiId);

        affectedProducts.forEach(updated -> {
            createAuditLog(auditInfo, updated);
            try {
                validateApiProductService.validateForDeploy(updated);
                deployApiProductDomainService.deploy(auditInfo, updated);
            } catch (ValidationDomainException e) {
                log.warn("API Product [{}] cannot be auto-deployed after API [{}] removal: {}", updated.getId(), apiId, e.getMessage());
            }
        });
    }

    private void createAuditLog(AuditInfo auditInfo, ApiProduct after) {
        auditDomainService.createApiProductAuditLog(
            ApiProductAuditLogEntity.builder()
                .organizationId(auditInfo.organizationId())
                .environmentId(auditInfo.environmentId())
                .event(ApiProductAuditEvent.API_PRODUCT_UPDATED)
                .apiProductId(after.getId())
                .actor(auditInfo.actor())
                .newValue(after)
                .createdAt(TimeProvider.now())
                .properties(Map.of(AuditProperties.API_PRODUCT, after.getId()))
                .build()
        );
    }
}
