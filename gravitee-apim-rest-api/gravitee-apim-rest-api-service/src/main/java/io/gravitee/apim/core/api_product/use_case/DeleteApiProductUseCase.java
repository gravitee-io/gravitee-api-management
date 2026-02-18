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
import io.gravitee.apim.core.api.domain_service.ApiStateDomainService;
import io.gravitee.apim.core.api_product.crud_service.ApiProductCrudService;
import io.gravitee.apim.core.api_product.domain_service.ValidateApiProductService;
import io.gravitee.apim.core.api_product.exception.ApiProductNotFoundException;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.api_product.query_service.ApiProductQueryService;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.ApiProductAuditLogEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.audit.model.event.ApiProductAuditEvent;
import io.gravitee.apim.core.event.crud_service.EventCrudService;
import io.gravitee.apim.core.event.crud_service.EventLatestCrudService;
import io.gravitee.apim.core.event.model.Event;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.model.EventType;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class DeleteApiProductUseCase {

    private final ApiProductCrudService apiProductCrudService;
    private final AuditDomainService auditService;
    private final ApiProductQueryService apiProductQueryService;
    private final ValidateApiProductService validateApiProductService;
    private final ApiStateDomainService apiStateDomainService;
    private final EventCrudService eventCrudService;
    private final EventLatestCrudService eventLatestCrudService;

    public void execute(Input input) {
        ApiProduct apiProduct = apiProductQueryService
            .findById(input.apiProductId())
            .orElseThrow(() -> new ApiProductNotFoundException(input.apiProductId()));

        if (apiProduct.getApiIds() != null && !apiProduct.getApiIds().isEmpty()) {
            for (var api : validateApiProductService.getApisToUndeployOnRemoval(apiProduct.getApiIds(), apiProduct.getId())) {
                apiStateDomainService.stop(api, input.auditInfo());
            }
        }

        publishUndeployEvent(input.auditInfo(), apiProduct);

        apiProductCrudService.delete(input.apiProductId());
        createAuditLog(input.apiProductId, input.auditInfo());
    }

    private void publishUndeployEvent(AuditInfo auditInfo, ApiProduct apiProduct) {
        final Event event = eventCrudService.createEvent(
            auditInfo.organizationId(),
            auditInfo.environmentId(),
            Set.of(auditInfo.environmentId()),
            EventType.UNDEPLOY_API_PRODUCT,
            apiProduct,
            Map.ofEntries(
                entry(Event.EventProperties.USER, auditInfo.actor().userId()),
                entry(Event.EventProperties.API_PRODUCT_ID, apiProduct.getId())
            )
        );

        eventLatestCrudService.createOrPatchLatestEvent(auditInfo.organizationId(), apiProduct.getId(), event);
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
