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
package io.gravitee.gateway.handlers.api.definition;

import static io.gravitee.repository.management.model.Plan.PlanSecurityType.*;

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.Plan;
import io.gravitee.definition.model.Policy;
import io.gravitee.definition.model.Rule;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.definition.model.flow.Step;
import io.gravitee.definition.model.plugins.resources.Resource;
import io.gravitee.gateway.reactor.handler.DefaultHttpAcceptor;
import io.gravitee.gateway.reactor.handler.HttpAcceptor;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class Api extends ReactableApi<io.gravitee.definition.model.Api> {

    public Api(io.gravitee.definition.model.Api api) {
        super(api);
    }

    @Override
    public <D> Set<D> dependencies(Class<D> type) {
        if (Policy.class.equals(type)) {
            return (Set<D>) policies();
        } else if (Resource.class.equals(type)) {
            return (Set<D>) new HashSet<>(definition.getResources());
        }

        return Collections.emptySet();
    }

    @Override
    public String getApiVersion() {
        return definition.getVersion();
    }

    @Override
    public DefinitionVersion getDefinitionVersion() {
        return definition.getDefinitionVersion();
    }

    private Set<Policy> policies() {
        Set<io.gravitee.definition.model.Policy> policies = new HashSet<>();
        // Load policies from the API
        if (definition.getPaths() != null) {
            definition
                .getPaths()
                .values()
                .forEach(rules -> policies.addAll(rules.stream().filter(Rule::isEnabled).map(Rule::getPolicy).collect(Collectors.toSet())));
        }

        // Load policies from Plans
        definition
            .getPlans()
            .forEach(
                plan -> {
                    String security = plan.getSecurity();
                    Policy secPolicy = buildSecurityPolicy(security);

                    if (secPolicy.getName() != null) {
                        policies.add(secPolicy);
                    }

                    if (plan.getPaths() != null) {
                        plan
                            .getPaths()
                            .values()
                            .forEach(
                                rules ->
                                    policies.addAll(rules.stream().filter(Rule::isEnabled).map(Rule::getPolicy).collect(Collectors.toSet()))
                            );
                    }

                    if (plan.getFlows() != null) {
                        plan
                            .getFlows()
                            .stream()
                            .filter(Flow::isEnabled)
                            .forEach(
                                flow -> {
                                    policies.addAll(getPolicies(flow.getPre()));
                                    policies.addAll(getPolicies(flow.getPost()));
                                }
                            );
                    }
                }
            );

        // Load policies from flows
        if (definition.getFlows() != null) {
            definition
                .getFlows()
                .stream()
                .filter(Flow::isEnabled)
                .forEach(
                    flow -> {
                        policies.addAll(getPolicies(flow.getPre()));
                        policies.addAll(getPolicies(flow.getPost()));
                    }
                );
        }

        return policies;
    }

    private Policy buildSecurityPolicy(String security) {
        Policy secPolicy = new Policy();
        switch (security) {
            case "KEY_LESS":
            case "key_less":
                secPolicy.setName("key-less");
                break;
            case "API_KEY":
            case "api_key":
                secPolicy.setName("api-key");
                break;
            case "OAUTH2":
                secPolicy.setName("oauth2");
                break;
            case "JWT":
                secPolicy.setName("jwt");
                break;
        }
        return secPolicy;
    }

    @Override
    public Set<String> getTags() {
        return definition.getTags();
    }

    @Override
    public String getId() {
        return definition.getId();
    }

    @Override
    public String getName() {
        return definition.getName();
    }

    @Override
    public Set<String> getSubscribablePlans() {
        return definition
            .getPlans()
            .stream()
            .filter(
                plan ->
                    OAUTH2.name().equalsIgnoreCase(plan.getSecurity()) ||
                    JWT.name().equalsIgnoreCase(plan.getSecurity()) ||
                    API_KEY.name().equalsIgnoreCase(plan.getSecurity())
            )
            .map(Plan::getId)
            .collect(Collectors.toSet());
    }

    @Override
    public Set<String> getApiKeyPlans() {
        return definition
            .getPlans()
            .stream()
            .filter(plan -> API_KEY.name().equalsIgnoreCase(plan.getSecurity()))
            .map(Plan::getId)
            .collect(Collectors.toSet());
    }

    private Collection<Policy> getPolicies(List<Step> flowStep) {
        if (flowStep == null || flowStep.isEmpty()) {
            return Collections.emptyList();
        }

        return flowStep
            .stream()
            .filter(Step::isEnabled)
            .map(
                step -> {
                    Policy policy = new Policy();
                    policy.setName(step.getPolicy());
                    policy.setConfiguration(step.getConfiguration());
                    return policy;
                }
            )
            .collect(Collectors.toList());
    }

    @Override
    public List<HttpAcceptor> httpAcceptors() {
        return definition
            .getProxy()
            .getVirtualHosts()
            .stream()
            .map(virtualHost -> new DefaultHttpAcceptor(virtualHost.getHost(), virtualHost.getPath()))
            .collect(Collectors.toList());
    }
}
