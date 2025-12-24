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
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.api_product.crud_service.ApiProductCrudService;
import io.gravitee.apim.core.api_product.exception.ApiProductNotFoundException;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.api_product.query_service.ApiProductQueryService;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.ApiProductAuditLogEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.audit.model.event.ApiProductAuditEvent;
import io.gravitee.common.utils.TimeProvider;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class DeleteApiFromApiProductUseCase {

    private final ApiProductQueryService apiProductQueryService;
    private final ApiProductCrudService apiProductCrudService;
    private final AuditDomainService auditService;

    public Output execute(Input input) {
        Optional<ApiProduct> apiProductOpt = apiProductQueryService.findById(input.apiProductId());
        if (apiProductOpt.isEmpty()) {
            throw new ApiProductNotFoundException(input.apiProductId());
        }

        ApiProduct apiProduct = apiProductOpt.get();

        ApiProduct beforeUpdate = apiProduct
            .toBuilder()
            .apiIds(apiProduct.getApiIds() != null ? new HashSet<>(apiProduct.getApiIds()) : null)
            .build();

        if (input.apiId() == null) {
            apiProduct.removeAllApiIds();
        } else {
            if (apiProduct.getApiIds() == null || !apiProduct.getApiIds().contains(input.apiId())) {
                throw new ApiNotFoundException(input.apiId());
            }
            apiProduct.removeApiId(input.apiId());
        }

        ApiProduct updated = apiProductCrudService.update(apiProduct);
        createAuditLog(beforeUpdate, updated, input.auditInfo());

        return new Output(updated);
    }

    public record Input(String apiProductId, String apiId, AuditInfo auditInfo) {
        public static Input of(String apiProductId, String apiId, AuditInfo auditInfo) {
            return new Input(apiProductId, apiId, auditInfo);
        }

        public static Input of(String apiProductId, AuditInfo auditInfo) {
            return new Input(apiProductId, null, auditInfo);
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
