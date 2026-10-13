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
package io.gravitee.repository.analytics.engine.api.query;

import java.util.Set;

/**
 * The calls a module scoped by API kind sees: every call of the APIs whose type is in scope, plus the calls
 * made through an entrypoint that adds its API to a kind in scope whatever the API's type (an MCP tool server
 * is a PROXY API, its {@code mcp} calls belong to MCP, its REST calls do not). Combined with the API
 * restriction every query carries, so the second arm never reaches an API the caller cannot read.
 *
 * <p>The search families whose documents carry no entrypoint apply the first arm alone: no traffic of an
 * entrypoint that adds a kind lands there.
 *
 * @param apiIds        APIs all of whose calls are in scope
 * @param entrypointIds entrypoints whose calls are in scope on any API, see {@link ObservabilityEntrypoints.AddedKind}
 */
public record TrafficScope(Set<String> apiIds, Set<String> entrypointIds) {
    public TrafficScope {
        apiIds = apiIds == null ? Set.of() : Set.copyOf(apiIds);
        entrypointIds = entrypointIds == null ? Set.of() : Set.copyOf(entrypointIds);
    }
}
