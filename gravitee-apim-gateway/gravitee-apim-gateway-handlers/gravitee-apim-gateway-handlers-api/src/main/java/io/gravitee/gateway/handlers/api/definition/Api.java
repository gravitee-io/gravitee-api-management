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

import static io.gravitee.gateway.handlers.api.definition.DefinitionContext.ORIGIN_KUBERNETES;

import io.gravitee.definition.model.Policy;
import io.gravitee.definition.model.Rule;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.definition.model.flow.Step;
import io.gravitee.definition.model.plugins.resources.Resource;
import io.gravitee.gateway.reactor.Reactable;
import io.gravitee.gateway.reactor.handler.Entrypoint;
import io.gravitee.gateway.reactor.handler.VirtualHost;
import java.io.Serializable;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class Api extends io.gravitee.definition.model.Api implements Reactable, Serializable {

    private boolean enabled = true;
    private Date deployedAt;

    private String environmentId;
    private String environmentHrid;
    private String organizationId;
    private String organizationHrid;

    private DefinitionContext definitionContext = new DefinitionContext();

    public Api() {}

    public Api(final io.gravitee.definition.model.Api definition) {
        this.setId(definition.getId());
        this.setName(definition.getName());
        this.setPathMappings(definition.getPathMappings());
        this.setPaths(definition.getPaths());
        this.setProperties(definition.getProperties());
        this.setProxy(definition.getProxy());
        this.setPathMappings(definition.getPathMappings());
        this.setResponseTemplates(definition.getResponseTemplates());
        this.setResources(definition.getResources());
        this.setServices(definition.getServices());
        this.setTags(definition.getTags());
        this.setVersion(definition.getVersion());
        this.setPlans(definition.getPlans());
        this.setDefinitionVersion(definition.getDefinitionVersion());
        this.setFlows(definition.getFlows());
        this.setFlowMode(definition.getFlowMode());
        this.setExecutionMode(definition.getExecutionMode());
    }

    public Api(final Api definition) {
        this.setId(definition.getId());
        this.setName(definition.getName());
        this.setPathMappings(definition.getPathMappings());
        this.setPaths(definition.getPaths());
        this.setProperties(definition.getProperties());
        this.setProxy(definition.getProxy());
        this.setPathMappings(definition.getPathMappings());
        this.setResponseTemplates(definition.getResponseTemplates());
        this.setResources(definition.getResources());
        this.setServices(definition.getServices());
        this.setTags(definition.getTags());
        this.setVersion(definition.getVersion());
        this.setPlans(definition.getPlans());
        this.setDefinitionVersion(definition.getDefinitionVersion());
        this.setFlows(definition.getFlows());
        this.setFlowMode(definition.getFlowMode());
        this.setEnvironmentId(definition.getEnvironmentId());
        this.setEnvironmentHrid(definition.getEnvironmentHrid());
        this.setOrganizationId(definition.getOrganizationId());
        this.setOrganizationHrid(definition.getOrganizationHrid());
        this.setExecutionMode(definition.getExecutionMode());
    }

    public DefinitionContext getDefinitionContext() {
        return definitionContext;
    }

    public void setDefinitionContext(DefinitionContext definitionContext) {
        this.definitionContext = definitionContext;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Date getDeployedAt() {
        return deployedAt;
    }

    public void setDeployedAt(Date deployedAt) {
        this.deployedAt = deployedAt;
    }

    @Override
    public boolean enabled() {
        return isEnabled();
    }

    @Override
    public <D> Set<D> dependencies(Class<D> type) {
        if (Policy.class.equals(type)) {
            return (Set<D>) policies();
        } else if (Resource.class.equals(type)) {
            return (Set<D>) new HashSet<>(getResources());
        }

        return Collections.emptySet();
    }

    private Set<Policy> policies() {
        Set<io.gravitee.definition.model.Policy> policies = new HashSet<>();

        if (ORIGIN_KUBERNETES.equals(this.getDefinitionContext().getOrigin())) {
            Policy secPolicy = buildSecurityPolicy("KEY_LESS");

            if (secPolicy.getName() != null) {
                policies.add(secPolicy);
            }
        }

        // Load policies from the API
        if (getPaths() != null) {
            getPaths()
                .values()
                .forEach(rules -> policies.addAll(rules.stream().filter(Rule::isEnabled).map(Rule::getPolicy).collect(Collectors.toSet())));
        }

        // Load policies from Plans
        getPlans()
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
                            .forEach(
                                new Consumer<Flow>() {
                                    @Override
                                    public void accept(Flow flow) {
                                        policies.addAll(getPolicies(flow.getPre()));
                                        policies.addAll(getPolicies(flow.getPost()));
                                    }
                                }
                            );
                    }
                }
            );

        // Load policies from flows
        if (getFlows() != null) {
            getFlows()
                .forEach(
                    new Consumer<Flow>() {
                        @Override
                        public void accept(Flow flow) {
                            policies.addAll(getPolicies(flow.getPre()));
                            policies.addAll(getPolicies(flow.getPost()));
                        }
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

    private Collection<Policy> getPolicies(List<Step> flowStep) {
        if (flowStep == null || flowStep.isEmpty()) {
            return Collections.emptyList();
        }

        return flowStep
            .stream()
            .map(
                new Function<Step, Policy>() {
                    @Override
                    public Policy apply(Step step) {
                        Policy policy = new Policy();
                        policy.setName(step.getPolicy());
                        policy.setConfiguration(step.getConfiguration());
                        return policy;
                    }
                }
            )
            .collect(Collectors.toList());
    }

    @Override
    public List<Entrypoint> entrypoints() {
        return getProxy()
            .getVirtualHosts()
            .stream()
            .map(virtualHost -> new VirtualHost(virtualHost.getHost(), virtualHost.getPath()))
            .collect(Collectors.toList());
    }

    public String getEnvironmentId() {
        return environmentId;
    }

    public void setEnvironmentId(String environmentId) {
        this.environmentId = environmentId;
    }

    public String getEnvironmentHrid() {
        return environmentHrid;
    }

    public void setEnvironmentHrid(String environmentHrid) {
        this.environmentHrid = environmentHrid;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    public String getOrganizationHrid() {
        return organizationHrid;
    }

    public void setOrganizationHrid(String organizationHrid) {
        this.organizationHrid = organizationHrid;
    }

    @Override
    public String toString() {
        return "API " + "id[" + this.getId() + "] name[" + this.getName() + "] version[" + this.getVersion() + ']';
    }
}
