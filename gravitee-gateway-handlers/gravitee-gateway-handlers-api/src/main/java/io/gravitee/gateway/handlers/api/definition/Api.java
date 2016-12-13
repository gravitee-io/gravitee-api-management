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

import io.gravitee.definition.model.Policy;
import io.gravitee.definition.model.Rule;
import io.gravitee.definition.model.plugins.resources.Resource;
import io.gravitee.gateway.reactor.Reactable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class Api extends io.gravitee.definition.model.Api implements Reactable<Api> {

    private boolean enabled = true;

    private Date deployedAt;

    private final Map<String, Plan> plans = new HashMap<>();

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

    public Plan getPlan(String plan) {
        return plans.get(plan);
    }

    public Collection<Plan> getPlans() {
        return plans.values();
    }

    public void setPlans(List<Plan> plans) {
        for(Plan plan : plans) {
            this.plans.put(plan.getId(), plan);
        }
    }

    @Override
    public Api item() {
        return this;
    }

    @Override
    public String contextPath() {
        return getProxy().getContextPath();
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
        if (getPaths() == null)
            return Collections.emptySet();

        Set<io.gravitee.definition.model.Policy> policies = new HashSet<>();

        // Load security policies
        registerSecurityPolicy(policies);

        // Load policies from the API
        getPaths().values()
                .forEach(path -> policies.addAll(
                        path.getRules()
                                .stream()
                                .map(Rule::getPolicy)
                                .distinct()
                                .collect(Collectors.toSet())));

        // Load policies from Plans
        getPlans().stream()
                .forEach(plan -> {
                    if (plan.getPaths() != null) {
                        plan.getPaths().values()
                                .forEach(path -> policies.addAll(
                                        path.getRules()
                                                .stream()
                                                .map(Rule::getPolicy)
                                                .distinct()
                                                .collect(Collectors.toSet())));
                    }});

        return policies;
    }

    private void registerSecurityPolicy(Set<io.gravitee.definition.model.Policy> policies) {
        // api-key policy
        Policy apiKey = new Policy();
        apiKey.setName("api-key");
        policies.add(apiKey);

        // keyless policy
        Policy keyless = new Policy();
        keyless.setName("key-less");
        policies.add(keyless);
    }

    @Override
    public Map<String, Object> properties() {
        return (getProperties() != null) ? Collections.unmodifiableMap(getProperties()) : Collections.emptyMap();
    }
}
