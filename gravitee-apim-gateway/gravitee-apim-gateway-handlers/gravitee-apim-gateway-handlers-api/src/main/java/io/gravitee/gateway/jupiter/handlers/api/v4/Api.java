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
package io.gravitee.gateway.jupiter.handlers.api.v4;

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.gateway.handlers.api.definition.ReactableApi;
import io.gravitee.gateway.reactor.handler.HttpAcceptor;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class Api extends ReactableApi<io.gravitee.definition.model.v4.Api> {

    public Api(io.gravitee.definition.model.v4.Api api) {
        super(api);
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
        // TODO
        return null;
    }

    @Override
    public Set<String> getApiKeyPlans() {
        // TODO
        return null;
    }

    @Override
    public <D> Set<D> dependencies(Class<D> type) {
        // TODO
        return Collections.emptySet();
    }

    @Override
    public List<HttpAcceptor> entrypoints() {
        return null;
    }
}
