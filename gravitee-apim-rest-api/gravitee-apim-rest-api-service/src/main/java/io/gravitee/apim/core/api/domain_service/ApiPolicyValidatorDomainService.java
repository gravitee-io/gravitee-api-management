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
package io.gravitee.apim.core.api.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.policy.domain_service.PolicyValidationDomainService;
import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.Plan;
import io.gravitee.definition.model.Rule;
import io.gravitee.definition.model.debug.DebugApiProxy;
import io.gravitee.definition.model.debug.DebugApiV4;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.definition.model.flow.Step;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@DomainService
public class ApiPolicyValidatorDomainService {

    private final PolicyValidationDomainService policyValidationDomainService;

    public void checkPolicyConfigurations(DebugApiProxy debugApiProxy) {
        if (debugApiProxy == null) {
            throw new IllegalStateException("Api should not be null");
        }

        if (debugApiProxy instanceof Api api) {
            var plans = new HashSet<Plan>(api.getPlans());
            switch (api.getDefinitionVersion()) {
                case V1 -> validatePathConfigurations(api, plans);
                case V2 -> validateFlowConfigurations(api, plans);
                case V4 -> throw new IllegalStateException("Cannot validate V4 api");
            }
        }

        if (debugApiProxy instanceof DebugApiV4 debugApiV4) {
            validateFlowV4Configurations(debugApiV4.getApiDefinition());
        }
    }

    private void validatePathConfigurations(Api api, Set<Plan> plans) {
        final Stream<Rule> pathsStream = getRulesStream(api, plans);

        if (pathsStream == null) {
            return;
        }

        pathsStream
            .filter(Rule::isEnabled)
            .map(Rule::getPolicy)
            .forEach(policy ->
                policy.setConfiguration(
                    policyValidationDomainService.validateAndSanitizeConfiguration(policy.getName(), policy.getConfiguration())
                )
            );
    }

    private static Stream<Rule> getRulesStream(Api api, Set<Plan> plans) {
        Stream<Rule> pathsStream = null;
        if (api.getPaths() != null) {
            pathsStream = api.getPaths().values().stream().flatMap(Collection::stream);
        }
        if (plans != null && pathsStream != null) {
            pathsStream = Stream.concat(
                pathsStream,
                plans
                    .stream()
                    .flatMap(plan ->
                        plan.getPaths() != null ? plan.getPaths().values().stream().flatMap(Collection::stream) : Stream.empty()
                    )
            );
        }
        return pathsStream;
    }

    private void validateFlowConfigurations(Api api, Set<Plan> plans) {
        final Stream<Step> flowsStream = getFlowsStream(api, plans);

        if (flowsStream == null) {
            return;
        }

        flowsStream
            .filter(Step::isEnabled)
            .forEach(step ->
                step.setConfiguration(
                    policyValidationDomainService.validateAndSanitizeConfiguration(step.getPolicy(), step.getConfiguration())
                )
            );
    }

    private static Stream<Step> getFlowsStream(Api api, Set<Plan> plans) {
        Stream<Flow> flowsStream = null;
        if (api.getFlows() != null) {
            flowsStream = api.getFlows().stream();
        }
        if (plans != null && flowsStream != null) {
            flowsStream = Stream.concat(
                flowsStream,
                plans.stream().flatMap(plan -> plan.getFlows() != null ? plan.getFlows().stream() : Stream.empty())
            );
        }

        if (flowsStream == null) {
            return null;
        }

        return flowsStream.flatMap(flow ->
            Stream.concat(
                flow.getPre() != null ? flow.getPre().stream() : Stream.empty(),
                flow.getPost() != null ? flow.getPost().stream() : Stream.empty()
            )
        );
    }

    private void validateFlowV4Configurations(io.gravitee.definition.model.v4.Api api) {
        var flowsStream = flowSteps(api);

        flowsStream
            .filter(io.gravitee.definition.model.v4.flow.step.Step::isEnabled)
            .forEach(step ->
                step.setConfiguration(
                    policyValidationDomainService.validateAndSanitizeConfiguration(step.getPolicy(), step.getConfiguration())
                )
            );
    }

    private static Stream<io.gravitee.definition.model.v4.flow.step.Step> flowSteps(io.gravitee.definition.model.v4.Api api) {
        var apiFlows = Optional.ofNullable(api.getFlows()).stream().flatMap(Collection::stream);
        var plansFlows = Optional.ofNullable(api.getPlans())
            .stream()
            .flatMap(plans -> plans.stream().flatMap(plan -> Optional.ofNullable(plan.getFlows()).stream().flatMap(Collection::stream)));

        return Stream.concat(apiFlows, plansFlows).flatMap(flow ->
            Stream.concat(
                Optional.ofNullable(flow.getRequest()).stream().flatMap(Collection::stream),
                Optional.ofNullable(flow.getResponse()).stream().flatMap(Collection::stream)
            )
        );
    }
}
