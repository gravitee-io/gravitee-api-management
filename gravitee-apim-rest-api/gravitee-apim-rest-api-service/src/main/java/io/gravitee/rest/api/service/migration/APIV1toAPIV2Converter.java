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

import static io.gravitee.common.http.HttpMethod.*;
import static io.gravitee.rest.api.service.validator.JsonHelper.clearNullValues;
import static java.util.Collections.reverseOrder;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.FlowMode;
import io.gravitee.definition.model.Rule;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.definition.model.flow.Operator;
import io.gravitee.definition.model.flow.PathOperator;
import io.gravitee.definition.model.flow.Step;
import io.gravitee.rest.api.model.PlanEntity;
import io.gravitee.rest.api.model.PlanStatus;
import io.gravitee.rest.api.model.PolicyEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class APIV1toAPIV2Converter {

    private static final Set<HttpMethod> HTTP_METHODS = Collections.unmodifiableSet(
        new HashSet<>(Arrays.asList(CONNECT, DELETE, GET, HEAD, OPTIONS, PATCH, POST, PUT, TRACE))
    );

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
    private List<Flow> migratePathsToFlows(Map<String, List<Rule>> paths, Set<PolicyEntity> policies) {
        List<Flow> flows = new ArrayList<>();
        if (!CollectionUtils.isEmpty(paths)) {
            paths.forEach(
                (pathKey, pathValue) -> {
                    // if all rules for a path have the same set of HttpMethods, then we have a unique flow for this path.
                    // else, we have a flow per rule in the path.
                    boolean oneFlowPerPathMode =
                        pathValue
                            .stream()
                            .map(
                                rule -> {
                                    Set<HttpMethod> methods = new HashSet<>(rule.getMethods());
                                    methods.retainAll(HTTP_METHODS);
                                    return methods;
                                }
                            )
                            .distinct()
                            .count() ==
                        1;

                    if (oneFlowPerPathMode) {
                        // since, all HttpMethods are the same in this case, we can use `pathValue.getRules().get(0).getMethods()`
                        final Flow flow = createFlow(pathKey, pathValue.get(0).getMethods());
                        pathValue.forEach(
                            rule -> {
                                configurePolicies(policies, rule, flow);
                            }
                        );

                        // reverse policies of the Post steps otherwise, flow are displayed in the wrong order into the policy studio
                        Collections.reverse(flow.getPost());
                        flows.add(flow);
                    } else {
                        pathValue.forEach(
                            rule -> {
                                final Flow flow = createFlow(pathKey, rule.getMethods());
                                configurePolicies(policies, rule, flow);

                                // reverse policies of the Post steps otherwise, flow are displayed in the wrong order into the policy studio
                                Collections.reverse(flow.getPost());
                                flows.add(flow);
                            }
                        );
                    }
                }
            );
        }

        return flows;
    }

    /**
     * Convert all plans related to Api to Plans with Flows.
     * @param plans, the collection of plan related to Api.
     * @param policies, the list of available policies, containing available scopes
     * @return the list of Plans
     */
    private Set<PlanEntity> migratePlans(Set<PlanEntity> plans, Set<PolicyEntity> policies) {
        return plans
            .stream()
            .filter(planEntity -> !PlanStatus.CLOSED.equals(planEntity.getStatus()))
            .map(
                planEntity -> {
                    final PlanEntity plan = new PlanEntity();
                    plan.setId(planEntity.getId());
                    plan.setApi(planEntity.getApi());
                    plan.setName(planEntity.getName());
                    plan.setSecurity(planEntity.getSecurity());
                    plan.setSecurityDefinition(planEntity.getSecurityDefinition());
                    plan.setValidation(planEntity.getValidation());
                    plan.setDescription(planEntity.getDescription());
                    plan.setType(planEntity.getType());
                    plan.setStatus(planEntity.getStatus());
                    plan.setFlows(migratePathsToFlows(planEntity.getPaths(), policies));
                    if (planEntity.getTags() != null) {
                        plan.setTags(new HashSet<>(planEntity.getTags()));
                    }
                    return plan;
                }
            )
            .collect(Collectors.toSet());
    }

    /**
     * Configure Flow's Steps from Rule.
     * @param policies, the list of available policies, containing available scopes
     * @param rule, the rule to transform into Step
     * @param flow, the current Flow
     */
    private void configurePolicies(Set<PolicyEntity> policies, Rule rule, Flow flow) {
        policies
            .stream()
            .filter(policy -> policy.getId().equals(rule.getPolicy().getName()))
            .findFirst()
            .ifPresent(
                policy -> {
                    String rulePolicyConfiguration = rule.getPolicy().getConfiguration();
                    String safeRulePolicyConfiguration = clearNullValues(rulePolicyConfiguration);

                    if (policy.getDevelopment().getOnRequestMethod() != null && policy.getDevelopment().getOnResponseMethod() != null) {
                        try {
                            JsonNode jsonRulePolicyConfiguration = JsonLoader.fromString(safeRulePolicyConfiguration);
                            JsonNode scope = jsonRulePolicyConfiguration.get("scope");
                            if (scope != null) {
                                switch (scope.asText()) {
                                    case "REQUEST":
                                    case "REQUEST_CONTENT":
                                        {
                                            final Step step = createStep(rule, policy, safeRulePolicyConfiguration);
                                            flow.getPre().add(step);
                                            break;
                                        }
                                    case "RESPONSE":
                                    case "RESPONSE_CONTENT":
                                        {
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
                }
            );
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
        final PathOperator pathOperator = new PathOperator();
        pathOperator.setPath(path);
        pathOperator.setOperator(Operator.STARTS_WITH);
        flow.setPathOperator(pathOperator);
        flow.setMethods(flowMethods);
        return flow;
    }

    /**
     * Reverse the order of the keys of the map to have the same behavior as previous Policy design.
     * @param pathsMap from ApiEntity
     * @return a NavigableMap with keys in reverse order
     */
    private NavigableMap<String, List<Rule>> reversePathsOrder(Map<String, List<Rule>> pathsMap) {
        TreeMap<String, List<Rule>> reversedPaths = new TreeMap<>(reverseOrder());
        reversedPaths.putAll(pathsMap);
        return reversedPaths.descendingMap();
    }
}
