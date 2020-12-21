/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.service.migration;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.FlowMode;
import io.gravitee.definition.model.Path;
import io.gravitee.definition.model.Plan;
import io.gravitee.definition.model.Rule;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.definition.model.flow.Operator;
import io.gravitee.definition.model.flow.Step;
import io.gravitee.rest.api.model.PlanEntity;
import io.gravitee.rest.api.model.PolicyEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static io.gravitee.common.http.HttpMethod.CONNECT;
import static io.gravitee.common.http.HttpMethod.DELETE;
import static io.gravitee.common.http.HttpMethod.GET;
import static io.gravitee.common.http.HttpMethod.HEAD;
import static io.gravitee.common.http.HttpMethod.OPTIONS;
import static io.gravitee.common.http.HttpMethod.PATCH;
import static io.gravitee.common.http.HttpMethod.POST;
import static io.gravitee.common.http.HttpMethod.PUT;
import static io.gravitee.common.http.HttpMethod.TRACE;
import static io.gravitee.rest.api.service.validator.PolicyCleaner.clearNullValues;
import static java.util.Collections.reverseOrder;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class APIV1toAPIV2Converter {

    private static final Set<HttpMethod> HTTP_METHODS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(CONNECT, DELETE, GET, HEAD, OPTIONS, PATCH, POST, PUT, TRACE)));

    public ApiEntity migrateToV2(final ApiEntity apiEntity, final Set<PolicyEntity> policies, Set<PlanEntity> plans) {

        apiEntity.setGraviteeDefinitionVersion(DefinitionVersion.V2.getLabel());
        apiEntity.setFlowMode(FlowMode.BEST_MATCH);

        apiEntity.setFlows(migratePathsToFlows(reversePathsOrder(apiEntity.getPaths()), policies));
        apiEntity.setPlans(migratePlans(plans, policies));

        // Reset 'paths' root collection as it is now managed by flows.
        apiEntity.setPaths(new HashMap<>());

        return apiEntity;
    }

    /**
     * Migrate apiEntity.paths to Flow model.
     * @param paths, the map of paths to migrate
     * @param policies, the list of available policies, containing available scopes
     * @return the list of Flows
     */
    private List<Flow> migratePathsToFlows(Map<String, Path> paths, Set<PolicyEntity> policies) {
        List<Flow> flows = new ArrayList<>();
        if (!CollectionUtils.isEmpty(paths)) {
            paths.forEach((pathKey, pathValue) -> {

                // if all rules for a path have the same set of HttpMethods, then we have a unique flow for this path.
                // else, we have a flow per rule in the path.
                boolean oneFlowPerPathMode =
                        pathValue
                        .getRules()
                        .stream()
                        .map(rule -> {
                            Set<HttpMethod> methods = new HashSet<>(rule.getMethods());
                            methods.retainAll(HTTP_METHODS);
                            return methods;
                        })
                        .distinct().count() == 1;

                if (oneFlowPerPathMode) {
                    // since, all HttpMethods are the same in this case, we can use `pathValue.getRules().get(0).getMethods()`
                    final Flow flow = createFlow(pathKey, pathValue.getRules().get(0).getMethods());
                    pathValue.getRules().forEach(rule -> {
                        configurePolicies(policies, rule, flow);
                    });
                    flows.add(flow);
                } else {
                    pathValue.getRules().forEach(rule -> {
                        final Flow flow = createFlow(pathKey, rule.getMethods());
                        configurePolicies(policies, rule, flow);
                        flows.add(flow);
                    });
                }
            });
        }

        return flows;
    }

    /**
     * Convert all plans related to Api to Plans with Flows.
     * @param plans, the collection of plan related to Api.
     * @param policies, the list of available policies, containing available scopes
     * @return the list of Plans
     */
    private List<Plan> migratePlans(Set<PlanEntity> plans, Set<PolicyEntity> policies) {
        return plans
                .stream()
                .map(planEntity -> {
                    final Plan plan = new Plan();
                    plan.setId(planEntity.getId());
                    plan.setName(planEntity.getName());
                    plan.setSecurity(planEntity.getSecurity().name());
                    plan.setStatus(planEntity.getStatus().name());
                    plan.setFlows(migratePathsToFlows(planEntity.getPaths(), policies));
                    return plan;
                })
                .collect(Collectors.toList());
    }

    /**
     * Configure Flow's Steps from Rule.
     * @param policies, the list of available policies, containing available scopes
     * @param rule, the rule to transform into Step
     * @param flow, the current Flow
     */
    private void configurePolicies(Set<PolicyEntity> policies, Rule rule, Flow flow) {
        policies.stream()
                .filter(policy -> policy.getId().equals(rule.getPolicy().getName()))
                .findFirst()
                .ifPresent(policy -> {
                    String rulePolicyConfiguration = rule.getPolicy().getConfiguration();
                    String safeRulePolicyConfiguration = clearNullValues(rulePolicyConfiguration);

                    if (policy.getDevelopment().getOnRequestMethod() != null
                            && policy.getDevelopment().getOnResponseMethod() != null) {

                        try {
                            JsonNode jsonRulePolicyConfiguration = JsonLoader.fromString(safeRulePolicyConfiguration);
                            JsonNode scope = jsonRulePolicyConfiguration.get("scope");
                            if (scope != null) {
                                switch (scope.asText()) {
                                    case "REQUEST":
                                    case "REQUEST_CONTENT": {
                                        final Step step = createStep(rule, policy, safeRulePolicyConfiguration);
                                        flow.getPre().add(step);
                                        break;
                                    }
                                    case "RESPONSE":
                                    case "RESPONSE_CONTENT": {
                                        final Step step = createStep(rule, policy, safeRulePolicyConfiguration);
                                        flow.getPost().add(step);
                                        break;
                                    }
                                }
                            }
                        } catch (IOException e) {
                            throw new InvalidDataException("Unable to validate policy configuration", e);
                        }
                    } else if (policy.getDevelopment().getOnRequestMethod() != null) {
                        final Step step = createStep(rule, policy, safeRulePolicyConfiguration);
                        flow.getPre().add(step);
                    } else if (policy.getDevelopment().getOnResponseMethod() != null) {
                        final Step step = createStep(rule, policy, safeRulePolicyConfiguration);
                        flow.getPost().add(step);
                    }
                });
    }

    @NotNull
    private Step createStep(Rule rule, PolicyEntity policy, String safeRulePolicyConfiguration) {
        final Step step = new Step();
        step.setName(policy.getName());
        step.setEnabled(rule.isEnabled());
        step.setDescription(rule.getDescription() != null ? rule.getDescription() : policy.getDescription());
        step.setPolicy(policy.getId());
        step.setConfiguration(safeRulePolicyConfiguration);
        return step;
    }

    @NotNull
    private Flow createFlow(String path, Set<HttpMethod> methods) {
        // If contains all methods of HttpMethod enum or all methods without OTHER
        Set<HttpMethod> flowMethods = methods.containsAll(HTTP_METHODS) ? Collections.emptySet() : methods;

        final Flow flow = new Flow();
        flow.setName("");
        flow.setCondition("");
        flow.setEnabled(true);
        flow.setPath(path);
        flow.setOperator(Operator.STARTS_WITH);
        flow.setMethods(flowMethods);
        return flow;
    }

    /**
     * Reverse the order of the keys of the map to have the same behavior as previous Policy design.
     * @param pathsMap from ApiEntity
     * @return a NavigableMap with keys in reverse order
     */
    private NavigableMap<String, Path> reversePathsOrder(Map<String, Path> pathsMap) {
        TreeMap<String, Path> reversedPaths = new TreeMap<>(reverseOrder());
        reversedPaths.putAll(pathsMap);
        return reversedPaths.descendingMap();
    }
}
