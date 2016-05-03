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
import io.gravitee.gateway.reactor.Reactable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david at gravitee.io)
 * @author GraviteeSource Team
 */
public class Api extends io.gravitee.definition.model.Api implements Reactable<Api> {

    private boolean enabled = true;

    private Date deployedAt;

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
    public Set<Policy> dependencies() {
        if (getPaths() == null)
            return Collections.EMPTY_SET;

        Set<io.gravitee.definition.model.Policy> policies = new HashSet<>();

            getPaths().values()
                    .forEach(path -> policies.addAll(
                            path.getRules()
                                    .stream()
                                    .map(Rule::getPolicy)
                                    .distinct()
                                    .collect(Collectors.toSet())));

        return policies;
    }

    @Override
    public Map<String, Object> properties() {
        return new HashMap(getProperties());
    }
}
