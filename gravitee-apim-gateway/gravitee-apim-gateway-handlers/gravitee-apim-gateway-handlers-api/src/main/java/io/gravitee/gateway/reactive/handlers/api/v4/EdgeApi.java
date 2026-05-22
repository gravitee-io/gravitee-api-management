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
package io.gravitee.gateway.reactive.handlers.api.v4;

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.gateway.reactor.AbstractReactableApi;
import java.util.Collections;
import java.util.Set;

public class EdgeApi extends AbstractReactableApi<io.gravitee.definition.model.v4.edge.EdgeApi> {

    public EdgeApi() {
        super();
    }

    public EdgeApi(io.gravitee.definition.model.v4.edge.EdgeApi edgeApi) {
        super(edgeApi);
    }

    @Override
    public String getApiVersion() {
        return definition.getApiVersion();
    }

    @Override
    public DefinitionVersion getDefinitionVersion() {
        return definition.getDefinitionVersion();
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
        return Collections.emptySet();
    }

    @Override
    public Set<String> getApiKeyPlans() {
        return Collections.emptySet();
    }

    @Override
    public <D> Set<D> dependencies(Class<D> type) {
        return Collections.emptySet();
    }
}
