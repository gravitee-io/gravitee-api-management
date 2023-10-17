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
package io.gravitee.gateway.platform.organization;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.gravitee.definition.model.FlowMode;
import io.gravitee.definition.model.Organization;
import io.gravitee.definition.model.Policy;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.definition.model.flow.Step;
import io.gravitee.gateway.reactor.Reactable;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class ReactableOrganization implements Reactable, Serializable {

    private Organization definition;
    private boolean enabled;
    private Date deployedAt;

    public ReactableOrganization(final io.gravitee.definition.model.Organization organization) {
        this.definition = Objects.requireNonNull(organization);
        this.deployedAt = new Date();
        this.enabled = true;
    }

    @Override
    public boolean enabled() {
        return isEnabled();
    }

    @Override
    public <D> Set<D> dependencies(Class<D> type) {
        if (Policy.class.equals(type)) {
            return (Set<D>) policies();
        }
        return Set.of();
    }

    private Set<Policy> policies() {
        Set<Policy> policies = new HashSet<>();

        // Load policies from flows
        if (definition.getFlows() != null) {
            definition
                .getFlows()
                .forEach(flow -> {
                    policies.addAll(getPolicies(flow.getPre()));
                    policies.addAll(getPolicies(flow.getPost()));
                });
        }

        return policies;
    }

    private Collection<Policy> getPolicies(List<Step> flowStep) {
        if (flowStep == null || flowStep.isEmpty()) {
            return Collections.emptyList();
        }

        return flowStep
            .stream()
            .map(step -> {
                Policy policy = new Policy();
                policy.setName(step.getPolicy());
                policy.setConfiguration(step.getConfiguration());
                return policy;
            })
            .toList();
    }

    @JsonIgnore
    public String getId() {
        return definition.getId();
    }

    @JsonIgnore
    public String getName() {
        return definition.getName();
    }

    @JsonIgnore
    public List<Flow> getFlows() {
        return definition.getFlows();
    }

    @JsonIgnore
    public void setFlows(final List<Flow> flows) {
        definition.setFlows(flows);
    }

    @JsonIgnore
    public FlowMode getFlowMode() {
        return definition.getFlowMode();
    }
}
