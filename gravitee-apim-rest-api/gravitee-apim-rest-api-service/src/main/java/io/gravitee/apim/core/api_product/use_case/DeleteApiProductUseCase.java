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
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.ApiProductAuditLogEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.audit.model.event.ApiProductAuditEvent;
import io.gravitee.common.utils.TimeProvider;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class DeleteApiProductUseCase {

    private final ApiProductCrudService apiProductCrudService;
    private final AuditDomainService auditService;

    public void execute(Input input) {
        apiProductCrudService.delete(input.apiProductId());
        createAuditLog(input.apiProductId, input.auditInfo());
    }

    public record Input(String apiProductId, AuditInfo auditInfo) {
        public static Input of(String apiProductId, AuditInfo auditInfo) {
            return new Input(apiProductId, auditInfo);
        }
    }

    private void createAuditLog(String apiProductId, AuditInfo auditInfo) {
        auditService.createApiProductAuditLog(
            ApiProductAuditLogEntity.builder()
                .organizationId(auditInfo.organizationId())
                .environmentId(auditInfo.environmentId())
                .event(ApiProductAuditEvent.API_PRODUCT_DELETED)
                .apiProductId(apiProductId)
                .actor(auditInfo.actor())
                .newValue(null)
                .createdAt(TimeProvider.now())
                .properties(Map.of(AuditProperties.API_PRODUCT, apiProductId))
                .build()
        );
    }
}
