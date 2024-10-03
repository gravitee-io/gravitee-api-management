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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.Rule;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.nativeapi.NativeApi;
import io.gravitee.definition.model.v4.nativeapi.NativePlan;
import io.gravitee.definition.model.v4.plan.AbstractPlan;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.model.Plan;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class PlanAppender {

    private final ObjectMapper objectMapper;
    private final PlanRepository planRepository;
    private final GatewayConfiguration gatewayConfiguration;

    /**
     * Fetching plans for given deployables
     * @param deployables the deployables to update
     * @return the deployables updated with plans
     */
    public List<ApiReactorDeployable> appends(final List<ApiReactorDeployable> deployables, final Set<String> environments) {
        // Fetch v1 api
        List<ApiReactorDeployable> v1ApiDeployables = deployables
            .stream()
            .filter(deployable -> deployable.reactableApi().getDefinitionVersion() == DefinitionVersion.V1)
            .collect(Collectors.toList());
        fetchV1ApiPlans(v1ApiDeployables, environments);

        // Fetch v1 api
        return deployables
            .stream()
            .map(deployable -> {
                ReactableApi<?> reactableApi = deployable.reactableApi();
                if (reactableApi.getDefinitionVersion() != DefinitionVersion.V4) {
                    filterPlanForApiV2(reactableApi);
                } else {
                    filterPlanForApiV4(reactableApi);
                }
                return deployable;
            })
            .filter(deployable -> {
                ReactableApi<?> reactableApi = deployable.reactableApi();
                boolean hasPlan = false;
                if (reactableApi.getDefinition() instanceof io.gravitee.definition.model.v4.Api v4Api) {
                    hasPlan = v4Api.getPlans() != null && !v4Api.getPlans().isEmpty();
                } else if (reactableApi.getDefinition() instanceof NativeApi nativeApi) {
                    hasPlan = nativeApi.getPlans() != null && !nativeApi.getPlans().isEmpty();
                } else if (reactableApi.getDefinition() instanceof io.gravitee.definition.model.Api api) {
                    hasPlan = api.getPlans() != null && !api.getPlans().isEmpty();
                }

                if (!hasPlan) {
                    log.warn("No plan found, skipping deployment for api: {}", deployable.apiId());
                }

                return hasPlan;
            })
            .collect(Collectors.toList());
    }

    private void fetchV1ApiPlans(final List<ApiReactorDeployable> deployables, final Set<String> environments) {
        final Map<String, Api> apiById = deployables
            .stream()
            .map(deployable -> (Api) deployable.reactableApi())
            .collect(Collectors.toMap(Api::getId, api -> api));

        // Get the api id to load plan only for V1 api definition.
        final List<String> apiV1Ids = new ArrayList<>(apiById.keySet());

        try {
            final Map<String, List<Plan>> plansByApi = planRepository
                .findByApisAndEnvironments(apiV1Ids, environments)
                .stream()
                .collect(Collectors.groupingBy(Plan::getApi));

            plansByApi.forEach((apiId, plans) -> {
                final Api api = apiById.get(apiId);
                api
                    .getDefinition()
                    .setPlans(
                        plans
                            .stream()
                            .filter(plan ->
                                Plan.Status.PUBLISHED.equals(plan.getStatus()) || Plan.Status.DEPRECATED.equals(plan.getStatus())
                            )
                            .map(this::convert)
                            .collect(Collectors.toList())
                    );
            });
        } catch (TechnicalException te) {
            log.error("Unexpected error while loading plans of APIs: [{}]", apiV1Ids, te);
        }
    }

    private io.gravitee.definition.model.Plan convert(Plan repoPlan) {
        io.gravitee.definition.model.Plan plan = new io.gravitee.definition.model.Plan();

        plan.setId(repoPlan.getId());
        plan.setName(repoPlan.getName());
        plan.setSecurityDefinition(repoPlan.getSecurityDefinition());
        plan.setSelectionRule(repoPlan.getSelectionRule());
        plan.setTags(repoPlan.getTags());
        plan.setStatus(repoPlan.getStatus().name());
        plan.setApi(repoPlan.getApi());

        if (repoPlan.getSecurity() != null) {
            plan.setSecurity(repoPlan.getSecurity().name());
        } else {
            // TODO: must be handle by a migration script
            plan.setSecurity("api_key");
        }

        try {
            if (repoPlan.getDefinition() != null && !repoPlan.getDefinition().trim().isEmpty()) {
                HashMap<String, List<Rule>> paths = objectMapper.readValue(repoPlan.getDefinition(), new TypeReference<>() {});
                plan.setPaths(paths);
            }
        } catch (IOException ioe) {
            log.error("Unexpected error while converting plan: {}", plan, ioe);
        }

        return plan;
    }

    private void filterPlanForApiV2(final ReactableApi<?> reactableApi) {
        io.gravitee.definition.model.Api apiDefinition = (io.gravitee.definition.model.Api) reactableApi.getDefinition();
        var plans = apiDefinition.getPlans();
        if (plans != null) {
            apiDefinition.setPlans(
                plans
                    .stream()
                    .filter(p -> p.getStatus() != null)
                    .filter(p -> filterPlanStatus(p.getStatus()))
                    .filter(p -> filterShardingTag(p.getName(), reactableApi.getName(), p.getTags()))
                    .collect(Collectors.toList())
            );
        }
    }

    private void filterPlanForApiV4(final ReactableApi<?> reactableApi) {
        io.gravitee.definition.model.v4.AbstractApi abstractApiDefinition =
            (io.gravitee.definition.model.v4.AbstractApi) reactableApi.getDefinition();
        if (ApiType.NATIVE != abstractApiDefinition.getType()) {
            var apiDefinition = (io.gravitee.definition.model.v4.Api) abstractApiDefinition;
            var plans = apiDefinition.getPlans();
            if (plans != null) {
                apiDefinition.setPlans(filterPlans(plans.stream(), reactableApi.getName()).collect(Collectors.toList()));
            }
        } else {
            var apiDefinition = (NativeApi) abstractApiDefinition;
            var plans = apiDefinition.getPlans();
            if (plans != null) {
                apiDefinition.setPlans(filterPlans(plans.stream(), reactableApi.getName()).collect(Collectors.toList()));
            }
        }
    }

    private <T extends AbstractPlan> Stream<T> filterPlans(Stream<T> planStream, String reactableName) {
        return planStream
            .filter(p -> p.getStatus() != null)
            .filter(p -> filterPlanStatus(p.getStatus().getLabel()))
            .filter(p -> filterShardingTag(p.getName(), reactableName, p.getTags()));
    }

    private boolean filterPlanStatus(final String planStatus) {
        return (
            PlanStatus.PUBLISHED.getLabel().equalsIgnoreCase(planStatus) || PlanStatus.DEPRECATED.getLabel().equalsIgnoreCase(planStatus)
        );
    }

    protected boolean filterShardingTag(final String planName, final String apiName, final Set<String> tags) {
        if (tags != null && !tags.isEmpty()) {
            boolean hasMatchingTags = gatewayConfiguration.hasMatchingTags(tags);
            if (!hasMatchingTags) {
                log.debug("Plan name[{}] api[{}] has been ignored because not in configured sharding tags", planName, apiName);
            }
            return hasMatchingTags;
        }
        return true;
    }
}
