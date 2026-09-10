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
package io.gravitee.rest.api.model.analytics;

import io.gravitee.common.http.HttpMethod;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.Builder;

@Builder(toBuilder = true)
public record SearchLogsFilters(
    Long from,
    Long to,
    Set<String> applicationIds,
    Set<String> planIds,
    Set<HttpMethod> methods,
    Set<String> mcpMethods,
    Set<Integer> statuses,
    List<StatusRange> statusRanges,
    Set<String> statusCodeGroups,
    Set<String> entrypointIds,
    Set<String> apiIds,
    Set<String> requestIds,
    Set<String> transactionIds,
    List<Range> responseTimeRanges,
    String uri,
    String bodyText,
    Set<String> errorKeys,
    Set<String> apiProductIds,
    Set<String> llmProxyModels,
    Set<String> llmProxyProviders,
    Set<String> mcpProxyTools,
    Set<String> mcpProxyResources,
    Set<String> mcpProxyPrompts,
    Set<String> nativeConnectionStatuses,
    Set<String> nativeClientIds,
    Set<String> nativeClientSoftwareNames,
    Set<String> nativeClientSoftwareVersions,
    Set<String> failureOrigins,
    Set<String> tenants,
    EntrypointScope entrypointScope
) {
    /**
     * An inclusive HTTP status code range bound for {@code HTTP_STATUS GTE/LTE}.
     */
    @Builder
    public record StatusRange(Integer gte, Integer lte) {}

    /**
     * Replaces {@code entrypointIds} when present: a default scope excludes the given ids (and keeps documents
     * without an entrypoint id by construction), an explicit condition selects exactly the given values, the
     * synthetic {@code (none)} value standing for documents without an entrypoint id.
     */
    public record EntrypointScope(Kind kind, List<String> ids) {
        public EntrypointScope {
            Objects.requireNonNull(kind, "An entrypoint scope needs a kind");
            ids = List.copyOf(ids);
        }

        public enum Kind {
            EXCLUDING,
            EXACTLY,
        }

        public static EntrypointScope excluding(Collection<String> ids) {
            return new EntrypointScope(Kind.EXCLUDING, List.copyOf(ids));
        }

        public static EntrypointScope exactly(Collection<String> values) {
            return new EntrypointScope(Kind.EXACTLY, List.copyOf(values));
        }
    }
}
