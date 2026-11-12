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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.api_product.model.ApiProductDeploymentPayload;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.event.crud_service.EventCrudService;
import io.gravitee.apim.core.event.crud_service.EventLatestCrudService;
import io.gravitee.apim.core.event.model.Event;
import io.gravitee.apim.core.event.query_service.EventLatestQueryService;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.flow.crud_service.FlowCrudService;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.plan.query_service.PlanQueryService;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class DeployApiProductDomainService {

    private static final String API_IDS = "apiIds";

    private final PlanQueryService planQueryService;
    private final EventCrudService eventCrudService;
    private final EventLatestCrudService eventLatestCrudService;
    private final EventLatestQueryService eventLatestQueryService;
    private final FlowCrudService flowCrudService;
    private final ObjectMapper objectMapper;

    public void deploy(AuditInfo auditInfo, ApiProduct apiProduct) {
        publish(auditInfo, apiProduct.getId(), currentPayload(apiProduct));
    }

    /**
     * Deploys a change to the API list alone. Everything else comes from the last deployed version, so flows, the
     * flow mode and plans saved since then go live only on an explicit deploy. A product never deployed is deployed whole.
     */
    public void deployApiList(AuditInfo auditInfo, ApiProduct apiProduct) {
        Object payload = lastDeployedPayload(apiProduct.getId())
            .<Object>map(deployed -> deployed.set(API_IDS, objectMapper.valueToTree(apiProduct.getApiIds())))
            .orElseGet(() -> currentPayload(apiProduct));
        publish(auditInfo, apiProduct.getId(), payload);
    }

    private ApiProductDeploymentPayload currentPayload(ApiProduct apiProduct) {
        var productPlans = planQueryService.findAllByReferenceIdAndReferenceType(
            apiProduct.getId(),
            GenericPlanEntity.ReferenceType.API_PRODUCT
        );
        var flowsByPlanId = flowCrudService.getPlanV4Flows(productPlans.stream().map(Plan::getId).collect(Collectors.toSet()));

        var plans = productPlans
            .stream()
            .map(plan -> {
                var definition = plan.getPlanDefinitionHttpV4();
                if (definition == null) {
                    return null;
                }
                definition.setFlows(flowsByPlanId.getOrDefault(plan.getId(), List.of()));
                return definition;
            })
            .filter(Objects::nonNull)
            .toList();

        return ApiProductDeploymentPayload.of(apiProduct, plans, flowCrudService.getApiProductFlows(apiProduct.getId()));
    }

    private Optional<ObjectNode> lastDeployedPayload(String apiProductId) {
        return eventLatestQueryService
            .findLatestByEntityIds(Set.of(apiProductId), EventType.DEPLOY_API_PRODUCT, Event.EventProperties.API_PRODUCT_ID)
            .stream()
            .map(Event::getPayload)
            .filter(payload -> payload != null && !payload.isBlank())
            .findFirst()
            .map(payload -> readPayload(apiProductId, payload));
    }

    private ObjectNode readPayload(String apiProductId, String payload) {
        try {
            return (ObjectNode) objectMapper.readTree(payload);
        } catch (JsonProcessingException | ClassCastException e) {
            throw new TechnicalDomainException("Unable to read the last deployed version of API Product " + apiProductId, e);
        }
    }

    private void publish(AuditInfo auditInfo, String apiProductId, Object payload) {
        final Event event = eventCrudService.createEvent(
            auditInfo.organizationId(),
            auditInfo.environmentId(),
            Set.of(auditInfo.environmentId()),
            EventType.DEPLOY_API_PRODUCT,
            payload,
            Map.ofEntries(
                entry(Event.EventProperties.USER, auditInfo.actor().userId()),
                entry(Event.EventProperties.API_PRODUCT_ID, apiProductId)
            )
        );

        eventLatestCrudService.createOrPatchLatestEvent(auditInfo.organizationId(), apiProductId, event);
    }
}
