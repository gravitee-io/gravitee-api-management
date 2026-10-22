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
package io.gravitee.gateway.handlers.guardian;

import io.gravitee.definition.model.Policy;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.definition.model.v4.guardian.Guardian;
import io.gravitee.gateway.reactor.Reactable;
import java.io.Serializable;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * A Guardian deployed on the gateway. It is handed to the Guardian reactor plugin through {@link io.gravitee.gateway.reactor.ReactorEvent}s.
 * <p>
 * Equality is restricted to the id and the environment: the {@link io.gravitee.gateway.reactor.handler.ReactorHandlerRegistry}
 * keys its handlers by reactable, and an updated copy of a Guardian must replace the deployed one.
 *
 * @author GraviteeSource Team
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class ReactableGuardian implements Reactable, Serializable {

    @EqualsAndHashCode.Include
    private String id;

    private String name;

    @EqualsAndHashCode.Include
    private String environmentId;

    private String environmentHrid;
    private String organizationId;
    private String organizationHrid;

    private Guardian definition;
    private Date deployedAt;

    public ReactableGuardian(Guardian definition) {
        this.definition = definition;
        this.id = definition.getId();
        this.name = definition.getName();
        this.environmentId = definition.getEnvironmentId();
        this.deployedAt = definition.getDeployedAt();
    }

    @Override
    public boolean enabled() {
        return true;
    }

    @Override
    public <D> Set<D> dependencies(Class<D> type) {
        if (Policy.class.equals(type)) {
            return (Set<D>) policies();
        }
        return Set.of();
    }

    private Set<Policy> policies() {
        if (definition == null || definition.getPolicies() == null) {
            return Set.of();
        }
        return definition
            .getPolicies()
            .stream()
            .filter(Step::isEnabled)
            .map(step -> {
                Policy policy = new Policy();
                policy.setName(step.getPolicy());
                policy.setConfiguration(step.getConfiguration());
                return policy;
            })
            .collect(Collectors.toSet());
    }
}
