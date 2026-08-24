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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.event.model.Event;
import io.gravitee.apim.core.event.query_service.EventLatestQueryService;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.plan.query_service.PlanQueryService;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

/**
 * Tells whether an API Product on screen still matches what was last deployed to the gateway.
 *
 * <p>A product is {@code DEPLOYED} when the last deploy event carries the same APIs and sharding tags it
 * carries now, and no non-staging plan has been changed since. Anything else — never deployed, an
 * unreadable payload, a since-changed plan — is reported as {@code NEED_REDEPLOY}, so the operator is
 * told to redeploy rather than told nothing.</p>
 *
 * <p>The state is resolved for a whole collection at a time and costs two queries however many products
 * are in it, so listing a page does not cost a query per row.</p>
 */
@DomainService
@RequiredArgsConstructor
@CustomLog
public class ApiProductDeploymentStateDomainService {

    private final EventLatestQueryService eventLatestQueryService;
    private final PlanQueryService planQueryService;
    private final ObjectMapper objectMapper;

    public ApiProduct computeDeploymentState(ApiProduct apiProduct) {
        computeDeploymentState(List.of(apiProduct));
        return apiProduct;
    }

    public void computeDeploymentState(Collection<ApiProduct> apiProducts) {
        if (apiProducts.isEmpty()) {
            return;
        }
        Set<String> productIds = apiProducts.stream().map(ApiProduct::getId).collect(Collectors.toSet());
        Set<String> environmentIds = apiProducts
            .stream()
            .map(ApiProduct::getEnvironmentId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Map<String, Event> latestDeployEvents = findLatestDeployEvents(productIds);
        Map<String, List<Plan>> plansByProductId = findPlansByProductId(productIds, environmentIds);

        apiProducts.forEach(product ->
            product.setDeploymentState(
                deploymentStateOf(
                    product,
                    latestDeployEvents.get(product.getId()),
                    plansByProductId.getOrDefault(product.getId(), List.of())
                )
            )
        );
    }

    private Map<String, Event> findLatestDeployEvents(Set<String> productIds) {
        return eventLatestQueryService
            .findLatestByEntityIds(productIds, EventType.DEPLOY_API_PRODUCT, Event.EventProperties.API_PRODUCT_ID)
            .stream()
            .filter(event -> event.getProperties() != null)
            .filter(event -> event.getProperties().get(Event.EventProperties.API_PRODUCT_ID) != null)
            .collect(
                Collectors.toMap(
                    event -> event.getProperties().get(Event.EventProperties.API_PRODUCT_ID),
                    Function.identity(),
                    (first, second) -> first
                )
            );
    }

    private Map<String, List<Plan>> findPlansByProductId(Set<String> productIds, Set<String> environmentIds) {
        if (environmentIds.isEmpty()) {
            return Map.of();
        }
        return planQueryService
            .findAllByReferenceIdsAndEnvironments(productIds, environmentIds, GenericPlanEntity.ReferenceType.API_PRODUCT)
            .stream()
            .collect(Collectors.groupingBy(Plan::getReferenceId));
    }

    private ApiProduct.DeploymentState deploymentStateOf(ApiProduct product, Event latestDeployEvent, List<Plan> plans) {
        try {
            if (latestDeployEvent == null || latestDeployEvent.getUpdatedAt() == null) {
                return ApiProduct.DeploymentState.NEED_REDEPLOY;
            }
            ApiProduct deployedProduct = readPayload(latestDeployEvent, product.getId());
            if (
                deployedProduct == null ||
                !setsEqual(product.getApiIds(), deployedProduct.getApiIds()) ||
                !setsEqual(product.getTags(), deployedProduct.getTags())
            ) {
                return ApiProduct.DeploymentState.NEED_REDEPLOY;
            }

            Instant lastDeployedAt = latestDeployEvent.getUpdatedAt().toInstant();
            boolean anyPlanModifiedAfterDeploy = plans
                .stream()
                .filter(plan -> plan.getPlanStatus() != PlanStatus.STAGING)
                .anyMatch(plan -> plan.getNeedRedeployAt() != null && plan.getNeedRedeployAt().toInstant().isAfter(lastDeployedAt));

            return anyPlanModifiedAfterDeploy ? ApiProduct.DeploymentState.NEED_REDEPLOY : ApiProduct.DeploymentState.DEPLOYED;
        } catch (Exception e) {
            log.warn("Failed to compute deployment state for API Product [{}]: {}", product.getId(), e.getMessage());
            return ApiProduct.DeploymentState.NEED_REDEPLOY;
        }
    }

    private ApiProduct readPayload(Event event, String productId) {
        String payload = event.getPayload();
        if (payload == null || payload.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(payload, ApiProduct.class);
        } catch (Exception e) {
            log.warn("Failed to deserialize deploy event payload for API Product [{}]: {}", productId, e.getMessage());
            return null;
        }
    }

    /**
     * True when two sets carry the same elements (e.g. apiIds or sharding tags vs last deploy payload).
     */
    private static boolean setsEqual(Set<String> current, Set<String> deployed) {
        if (current == null && deployed == null) return true;
        if (current == null || deployed == null) return false;
        return current.size() == deployed.size() && current.containsAll(deployed);
    }
}
