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
package io.gravitee.gamma.rest.core.observability.logs.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.gamma.rest.core.observability.filter.model.ApiType;
import io.gravitee.gamma.rest.core.observability.logs.port.service_provider.ObservabilityLogsDataPort.AccessibleApi;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Computes the effective API scope for an observability query by intersecting the caller's
 * RBAC-accessible APIs with the signal's supported API kinds and any user-supplied API filter.
 *
 * <p>This encapsulation avoids duplicating the scoping logic across logs, analytics, and future
 * observability consumers. The service is stateless — all inputs are passed explicitly so it can
 * be unit-tested without infrastructure.
 *
 * @author GraviteeSource Team
 */
@DomainService
public class AccessibleApiScopeDomainService {

    public record ScopedApis(Set<String> apiIds, Map<String, String> apiNamesById) {}

    /**
     * @param accessibleApis   All APIs the caller can read (from the data port).
     * @param wantedApiTypes   API kinds relevant for the current signal (e.g. PROXY, LLM_PROXY,
     *                         MCP_PROXY for LOGS). Extensible: adding MESSAGE or NATIVE later only
     *                         requires widening this set.
     * @param userApiFilter    API IDs explicitly requested by the caller (from an {@code API}
     *                         filter condition). {@code null} or empty means "all accessible".
     * @return The intersection of the three constraints. An empty {@code apiIds} means the caller
     *         has no data to see (→ empty result, not 403).
     */
    public ScopedApis computeScope(List<AccessibleApi> accessibleApis, Set<ApiType> wantedApiTypes, Set<String> userApiFilter) {
        var filtered = accessibleApis.stream().filter(api -> api.type() != null && wantedApiTypes.contains(api.type()));

        if (userApiFilter != null && !userApiFilter.isEmpty()) {
            filtered = filtered.filter(api -> userApiFilter.contains(api.id()));
        }

        var apiList = filtered.toList();
        var ids = apiList.stream().map(AccessibleApi::id).collect(Collectors.toSet());
        var names = apiList.stream().collect(Collectors.toMap(AccessibleApi::id, AccessibleApi::name, (a, b) -> a));

        return new ScopedApis(ids, names);
    }
}
