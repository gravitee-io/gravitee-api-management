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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.api_product.model.ApiProductDeploymentPayload;
import io.gravitee.apim.core.event.model.Event;
import io.gravitee.apim.core.event.query_service.EventLatestQueryService;
import io.gravitee.apim.core.flow.crud_service.FlowCrudService;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.plan.query_service.PlanQueryService;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.execution.FlowExecution;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

/**
 * Tells whether an API Product on screen still matches what was last deployed to the gateway.
 *
 * <p>A product is {@code DEPLOYED} when the last deploy event carries the payload a deploy would write now.
 * Anything else — never deployed, an unreadable payload, any difference — is reported as {@code NEED_REDEPLOY},
 * so the operator is told to redeploy rather than told nothing.</p>
 *
 * <p>The state is resolved for a whole collection at a time and costs four queries however many products
 * are in it, so listing a page does not cost a query per row.</p>
 */
@DomainService
@RequiredArgsConstructor
@CustomLog
public class ApiProductDeploymentStateDomainService {

    private static final String PLANS = "plans";
    private static final String FLOWS = "flows";
    private static final String FLOW_EXECUTION = "flowExecution";
    private static final String TAGS = "tags";
    private static final String STATUS = "status";
    private static final String NAME = "name";
    private static final String RESOURCES = "resources";
    /** As for an API, these change what operators see, not what the gateway runs, so they never call for a redeploy. */
    private static final Set<String> DISPLAY_FIELDS = Set.of(NAME, "description", "version");
    private static final Set<String> SET_FIELDS = Set.of("apiIds", TAGS);

    private final EventLatestQueryService eventLatestQueryService;
    private final PlanQueryService planQueryService;
    private final FlowCrudService flowCrudService;
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
        Map<String, List<Flow>> flowsByProductId = flowCrudService.getApiProductFlows(productIds);
        Map<String, List<Flow>> flowsByPlanId = flowCrudService.getPlanV4Flows(
            plansByProductId.values().stream().flatMap(List::stream).map(Plan::getId).collect(Collectors.toSet())
        );

        apiProducts.forEach(product ->
            product.setDeploymentState(
                deploymentStateOf(
                    product,
                    flowsByProductId.getOrDefault(product.getId(), List.of()),
                    latestDeployEvents.get(product.getId()),
                    plansByProductId.getOrDefault(product.getId(), List.of()),
                    flowsByPlanId
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

    private ApiProduct.DeploymentState deploymentStateOf(
        ApiProduct product,
        List<Flow> flows,
        Event latestDeployEvent,
        List<Plan> plans,
        Map<String, List<Flow>> flowsByPlanId
    ) {
        try {
            if (latestDeployEvent == null) {
                return ApiProduct.DeploymentState.NEED_REDEPLOY;
            }
            JsonNode deployed = readPayload(latestDeployEvent, product.getId());
            var planDefinitions = plans
                .stream()
                .map(plan -> {
                    var definition = plan.getPlanDefinitionHttpV4();
                    if (definition != null) {
                        definition.setFlows(flowsByPlanId.getOrDefault(plan.getId(), List.of()));
                    }
                    return definition;
                })
                .filter(Objects::nonNull)
                .toList();
            // Read back from its JSON, as the deployed side is: step and security configurations are raw JSON values,
            // which a tree built from the objects would hold as raw strings rather than as objects.
            JsonNode current = objectMapper.readTree(
                objectMapper.writeValueAsString(ApiProductDeploymentPayload.of(product, planDefinitions, flows))
            );
            return deployed != null && comparable(deployed).equals(comparable(current))
                ? ApiProduct.DeploymentState.DEPLOYED
                : ApiProduct.DeploymentState.NEED_REDEPLOY;
        } catch (Exception e) {
            log.warn("Failed to compute deployment state for API Product [{}]: {}", product.getId(), e.getClass().getSimpleName());
            return ApiProduct.DeploymentState.NEED_REDEPLOY;
        }
    }

    private JsonNode readPayload(Event event, String productId) {
        String payload = event.getPayload();
        if (payload == null || payload.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(payload);
        } catch (Exception e) {
            log.warn("Failed to deserialize deploy event payload for API Product [{}]: {}", productId, e.getClass().getSimpleName());
            return null;
        }
    }

    /**
     * The payload as the gateway would run it. Display fields and STAGING plans, which the gateway ignores, are left out.
     * A payload deployed before product flows existed has no flows and no mode, which the gateway reads as none and the
     * default mode. Plans are compared by id and resources by name, whatever order they were saved in.
     */
    private ObjectNode comparable(JsonNode payload) {
        var node = (ObjectNode) withoutNullFields(payload);
        node.remove(DISPLAY_FIELDS);
        SET_FIELDS.forEach(field -> node.set(field, sorted(node.get(field))));
        node.putIfAbsent(PLANS, node.arrayNode());
        if (node.get(PLANS) instanceof ArrayNode plans) {
            node.set(PLANS, gatewayPlans(plans));
        }
        if (node.get(RESOURCES) instanceof ArrayNode resources) {
            node.set(RESOURCES, sortedBy(StreamSupport.stream(resources.spliterator(), false), NAME));
        }
        node.putIfAbsent(FLOWS, node.arrayNode());
        node.putIfAbsent(FLOW_EXECUTION, objectMapper.valueToTree(new FlowExecution()));
        return node;
    }

    private static JsonNode withoutNullFields(JsonNode node) {
        if (node instanceof ObjectNode object) {
            var copy = object.objectNode();
            object
                .properties()
                .stream()
                .filter(field -> !field.getValue().isNull())
                .forEach(field -> copy.set(field.getKey(), withoutNullFields(field.getValue())));
            return copy;
        }
        if (node instanceof ArrayNode array) {
            var copy = array.arrayNode();
            array.forEach(element -> copy.add(withoutNullFields(element)));
            return copy;
        }
        return node;
    }

    private static ArrayNode gatewayPlans(ArrayNode plans) {
        return sortedBy(
            StreamSupport.stream(plans.spliterator(), false)
                .filter(plan -> !PlanStatus.STAGING.getLabel().equals(plan.path(STATUS).asText()))
                .map(ApiProductDeploymentStateDomainService::asRun),
            "id"
        );
    }

    private static ArrayNode sortedBy(Stream<JsonNode> nodes, String field) {
        var result = JsonNodeFactory.instance.arrayNode();
        nodes.sorted(Comparator.comparing((JsonNode node) -> node.path(field).asText())).forEach(result::add);
        return result;
    }

    private static JsonNode asRun(JsonNode plan) {
        if (plan instanceof ObjectNode object) {
            object.remove(NAME);
            if (object.has(TAGS)) {
                object.set(TAGS, sorted(object.get(TAGS)));
            }
        }
        return plan;
    }

    private static ArrayNode sorted(JsonNode values) {
        var sorted = JsonNodeFactory.instance.arrayNode();
        if (values != null) {
            StreamSupport.stream(values.spliterator(), false).sorted(Comparator.comparing(JsonNode::asText)).forEach(sorted::add);
        }
        return sorted;
    }
}
