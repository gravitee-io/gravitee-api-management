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
package io.gravitee.gamma.rest.infra.adapter;

import io.gravitee.gamma.rest.core.observability.filter.model.ApiType;
import io.gravitee.gamma.rest.core.observability.filter.port.service_provider.EntrypointScopeProvider;
import io.gravitee.repository.analytics.engine.api.query.ObservabilityEntrypoints;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Reads the entrypoint scopes from {@link ObservabilityEntrypoints}, the declaration the analytics
 * engine's own default scoping is built from, so Gamma and the engine cannot drift apart.
 *
 * @author GraviteeSource Team
 */
public class EntrypointScopeProviderAdapter implements EntrypointScopeProvider {

    @Override
    public List<String> excludedFromLogs() {
        return ObservabilityEntrypoints.LOGS_EXCLUDED_IDS;
    }

    /** The registry names its kinds after the {@code API_TYPE} values, so a Gamma kind maps by name. */
    @Override
    public Set<String> entrypointsAdding(Set<ApiType> kinds) {
        return Arrays.stream(ObservabilityEntrypoints.AddedKind.values())
            .filter(added -> kinds.stream().anyMatch(kind -> kind.name().equals(added.name())))
            .flatMap(added -> ObservabilityEntrypoints.idsAdding(added).stream())
            .collect(Collectors.toUnmodifiableSet());
    }
}
