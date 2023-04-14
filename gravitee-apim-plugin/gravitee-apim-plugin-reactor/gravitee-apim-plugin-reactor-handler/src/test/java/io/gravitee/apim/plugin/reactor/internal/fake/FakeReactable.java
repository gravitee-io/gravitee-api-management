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
package io.gravitee.apim.plugin.reactor.internal.fake;

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.gateway.reactor.Reactable;
import io.gravitee.gateway.reactor.ReactableApi;
import java.util.Set;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class FakeReactable extends ReactableApi<Object> {

    protected FakeReactable(Object definition) {
        super(definition);
    }

    @Override
    public String getApiVersion() {
        return null;
    }

    @Override
    public boolean enabled() {
        return true;
    }

    @Override
    public DefinitionVersion getDefinitionVersion() {
        return null;
    }

    @Override
    public Set<String> getTags() {
        return Set.of();
    }

    @Override
    public String getId() {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public Set<String> getSubscribablePlans() {
        return Set.of();
    }

    @Override
    public Set<String> getApiKeyPlans() {
        return Set.of();
    }

    @Override
    public <D> Set<D> dependencies(Class<D> type) {
        return Set.of();
    }
}
