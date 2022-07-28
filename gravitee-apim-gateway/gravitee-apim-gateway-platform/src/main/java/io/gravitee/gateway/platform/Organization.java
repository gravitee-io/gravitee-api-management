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
package io.gravitee.gateway.platform;

import io.gravitee.definition.model.Policy;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.definition.model.flow.Step;
import io.gravitee.gateway.policy.PolicyDefinition;

import java.io.Serializable;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
public class Organization extends io.gravitee.definition.model.Organization implements Serializable {

    public Organization(io.gravitee.definition.model.Organization eventOrganization) {
        this.setId(eventOrganization.getId());
        this.setName(eventOrganization.getName());
        this.setDescription(eventOrganization.getDescription());
        this.setDomainRestrictions(eventOrganization.getDomainRestrictions());
        this.setFlowMode(eventOrganization.getFlowMode());
        this.setFlows(eventOrganization.getFlows());
        this.setHrids(eventOrganization.getHrids());
        this.setUpdatedAt(eventOrganization.getUpdatedAt());
    }

    public <D> Set<D> dependencies(Class<D> type) {
        if (PolicyDefinition.class.equals(type)) {
            return (Set<D>) policies();
        }
        return Collections.emptySet();
    }

    private Set<PolicyDefinition> policies() {
        Set<PolicyDefinition> policies = new HashSet<>();

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

    private Collection<PolicyDefinition> getPolicies(List<Step> flowStep) {
        if (flowStep == null || flowStep.isEmpty()) {
            return Collections.emptyList();
        }

        return flowStep
            .stream()
            .map(
                step -> {
                    Policy policy = new Policy();
                    policy.setName(step.getPolicy());
                    policy.setConfiguration(step.getConfiguration());
                    return policy;
                }
            )
                .map(policy -> new PolicyDefinition(policy.getName(), policy.getConfiguration()))
            .collect(Collectors.toList());
    }
}
