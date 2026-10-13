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
import io.gravitee.gamma.rest.core.observability.logs.model.ApiReference;
import io.gravitee.gamma.rest.core.observability.logs.model.TrafficScope;
import io.gravitee.gamma.rest.core.observability.logs.port.service_provider.ObservabilityLogsDataPort.AccessibleApi;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Computes the effective API scope for an observability query from the caller's RBAC-accessible APIs,
 * the wanted API kinds and any user-supplied API filter. When an entrypoint adds a wanted kind to APIs
 * of another type, the scope keeps every candidate API and narrows their calls instead.
 *
 * <p>This encapsulation avoids duplicating the scoping logic across logs, analytics, and future
 * observability consumers. The service is stateless — all inputs are passed explicitly so it can
 * be unit-tested without infrastructure.
 *
 * @author GraviteeSource Team
 */
@DomainService
public class AccessibleApiScopeDomainService {

    /**
     * @param apiIds       The accessible API ids in scope.
     * @param apisById     Per-API enrichment context (display name + canonical wire {@code apiType}),
     *                     keyed by API id.
     * @param trafficScope Which calls of {@code apiIds} the search sees when an entrypoint adds one of the wanted
     *                     kinds to an API of another type; {@code null} when the API type alone decides.
     */
    public record ScopedApis(Set<String> apiIds, Map<String, ApiReference> apisById, TrafficScope trafficScope) {}

    /** {@link #computeScope(List, Set, Set, Set)} for wanted kinds no entrypoint adds to an API of another type. */
    public ScopedApis computeScope(List<AccessibleApi> accessibleApis, Set<ApiType> wantedApiTypes, Set<String> userApiFilter) {
        return computeScope(accessibleApis, wantedApiTypes, userApiFilter, Set.of());
    }

    /**
     * @param accessibleApis    All APIs the caller can read (from the data port).
     * @param wantedApiTypes    API kinds the caller may see for the current request: every kind by
     *                          default, or on logs the kinds an {@code API_TYPE} condition names.
     *                          Analytics narrows by its {@code API_TYPE} conditions afterwards, per query.
     * @param userApiFilter     API IDs explicitly requested by the caller (from the {@code API} filter
     *                          condition of a logs search). {@code null} or empty means "all accessible".
     * @param addingEntrypoints Entrypoints whose calls belong to a wanted kind whatever the API's type. When
     *                          there are some, every accessible API may have such calls: the scope keeps them all
     *                          and its {@link TrafficScope} narrows their calls.
     * @return The intersection of the constraints. An empty {@code apiIds} means the caller
     *         has no data to see (→ empty result, not 403).
     */
    public ScopedApis computeScope(
        List<AccessibleApi> accessibleApis,
        Set<ApiType> wantedApiTypes,
        Set<String> userApiFilter,
        Set<String> addingEntrypoints
    ) {
        var candidates = accessibleApis
            .stream()
            .filter(api -> api.type() != null)
            .filter(api -> userApiFilter == null || userApiFilter.isEmpty() || userApiFilter.contains(api.id()))
            .toList();
        return scopeOf(candidates, wantedApiTypes, addingEntrypoints);
    }

    /**
     * The kind rule logs and analytics share: the candidates of the wanted types, or, when an entrypoint adds a
     * wanted kind to APIs of another type, every candidate with a {@link TrafficScope} narrowing their calls.
     */
    public static ScopedApis scopeOf(List<AccessibleApi> candidates, Set<ApiType> wantedApiTypes, Set<String> addingEntrypoints) {
        var ofWantedTypes = candidates
            .stream()
            .filter(api -> wantedApiTypes.contains(api.type()))
            .toList();

        if (addingEntrypoints == null || addingEntrypoints.isEmpty()) {
            return new ScopedApis(idsOf(ofWantedTypes), referencesOf(ofWantedTypes), null);
        }
        return new ScopedApis(idsOf(candidates), referencesOf(candidates), new TrafficScope(idsOf(ofWantedTypes), addingEntrypoints));
    }

    private static Set<String> idsOf(List<AccessibleApi> apis) {
        return apis.stream().map(AccessibleApi::id).collect(Collectors.toSet());
    }

    private static Map<String, ApiReference> referencesOf(List<AccessibleApi> apis) {
        return apis.stream().collect(Collectors.toMap(AccessibleApi::id, AccessibleApiScopeDomainService::toReference, (a, b) -> a));
    }

    private static ApiReference toReference(AccessibleApi api) {
        // apiType is emitted as the canonical enum name (uppercase, e.g. "HTTP_PROXY") to match the
        // documented log-row contract and the API_TYPE filter values.
        return new ApiReference(api.name(), api.type() != null ? api.type().name() : null);
    }
}
