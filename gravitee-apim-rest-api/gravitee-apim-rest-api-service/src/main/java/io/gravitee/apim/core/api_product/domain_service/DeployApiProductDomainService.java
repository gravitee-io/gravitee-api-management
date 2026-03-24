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

import static java.util.Map.entry;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.api_product.model.ApiProductDeploymentPayload;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.event.crud_service.EventCrudService;
import io.gravitee.apim.core.event.crud_service.EventLatestCrudService;
import io.gravitee.apim.core.event.model.Event;
import io.gravitee.apim.core.plan.query_service.PlanQueryService;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class DeployApiProductDomainService {

    private final PlanQueryService planQueryService;
    private final EventCrudService eventCrudService;
    private final EventLatestCrudService eventLatestCrudService;

    public void deploy(AuditInfo auditInfo, ApiProduct apiProduct) {
        var plans = planQueryService
            .findAllByReferenceIdAndReferenceType(apiProduct.getId(), GenericPlanEntity.ReferenceType.API_PRODUCT)
            .stream()
            .map(io.gravitee.apim.core.plan.model.Plan::getPlanDefinitionHttpV4)
            .filter(Objects::nonNull)
            .toList();

        var payload = ApiProductDeploymentPayload.builder()
            .id(apiProduct.getId())
            .name(apiProduct.getName())
            .description(apiProduct.getDescription())
            .version(apiProduct.getVersion())
            .apiIds(apiProduct.getApiIds())
            .environmentId(apiProduct.getEnvironmentId())
            .plans(plans)
            .build();

        final Event event = eventCrudService.createEvent(
            auditInfo.organizationId(),
            auditInfo.environmentId(),
            Set.of(auditInfo.environmentId()),
            EventType.DEPLOY_API_PRODUCT,
            payload,
            Map.ofEntries(
                entry(Event.EventProperties.USER, auditInfo.actor().userId()),
                entry(Event.EventProperties.API_PRODUCT_ID, apiProduct.getId())
            )
        );

        eventLatestCrudService.createOrPatchLatestEvent(auditInfo.organizationId(), apiProduct.getId(), event);
    }
}
