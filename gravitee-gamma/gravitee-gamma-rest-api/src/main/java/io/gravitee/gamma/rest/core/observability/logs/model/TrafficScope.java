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
package io.gravitee.gamma.rest.core.observability.logs.model;

import java.util.Set;

/**
 * The calls a search scoped by API kind sees, on top of its API ids: every call of {@code apiIds}, the APIs its
 * kinds name by type, plus the calls made through one of {@code entrypointIds} on any API, the entrypoints that
 * add an API to one of its kinds whatever the API's type (an MCP tool server is a PROXY API: its {@code mcp}
 * calls belong to MCP, its REST calls do not).
 */
public record TrafficScope(Set<String> apiIds, Set<String> entrypointIds) {
    public TrafficScope {
        apiIds = apiIds == null ? Set.of() : Set.copyOf(apiIds);
        entrypointIds = entrypointIds == null ? Set.of() : Set.copyOf(entrypointIds);
    }
}
