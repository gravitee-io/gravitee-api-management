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
package io.gravitee.gateway.handlers.sharedpolicygroup;

import io.gravitee.definition.model.Policy;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.definition.model.v4.sharedpolicygroup.SharedPolicyGroup;
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
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class ReactableSharedPolicyGroup implements Reactable, Serializable {

    @EqualsAndHashCode.Exclude
    private String id;

    private String name;
    private String environmentId;
    private String environmentHrid;
    private String organizationId;
    private String organizationHrid;

    private SharedPolicyGroup definition;
    private Date deployedAt;

    public ReactableSharedPolicyGroup(SharedPolicyGroup definition) {
        this.definition = definition;
        this.environmentId = definition.getEnvironmentId();
        this.id = definition.getId();
    }

    @Override
    public boolean enabled() {
        // No reason to have this object disabled.
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
        if (definition == null || definition.getPolicies() == null || definition.getPolicies().isEmpty()) {
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
