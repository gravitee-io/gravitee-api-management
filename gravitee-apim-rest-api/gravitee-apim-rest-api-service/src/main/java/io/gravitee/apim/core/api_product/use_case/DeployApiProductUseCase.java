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
import io.gravitee.apim.core.api_product.exception.ApiProductNotFoundException;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.api_product.query_service.ApiProductQueryService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.event.crud_service.EventCrudService;
import io.gravitee.apim.core.event.crud_service.EventLatestCrudService;
import io.gravitee.apim.core.event.model.Event;
import io.gravitee.rest.api.model.EventType;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;

/**
 * Publishes DEPLOY_API_PRODUCT event to trigger gateway sync.
 * Use when an API Product needs to be deployed or redeployed without updating its definition.
 */
@UseCase
@RequiredArgsConstructor
public class DeployApiProductUseCase {

    private final ApiProductQueryService apiProductQueryService;
    private final EventCrudService eventCrudService;
    private final EventLatestCrudService eventLatestCrudService;

    public Output execute(Input input) {
        Optional<ApiProduct> apiProductOpt = apiProductQueryService.findById(input.apiProductId());
        if (apiProductOpt.isEmpty()) {
            throw new ApiProductNotFoundException(input.apiProductId());
        }
        ApiProduct apiProduct = apiProductOpt.get();
        publishDeployEvent(input.auditInfo(), apiProduct);
        return new Output(apiProduct);
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

    public record Input(String apiProductId, AuditInfo auditInfo) {
        public static Input of(String apiProductId, AuditInfo auditInfo) {
            return new Input(apiProductId, auditInfo);
        }
    }

    public record Output(ApiProduct apiProduct) {}
}
