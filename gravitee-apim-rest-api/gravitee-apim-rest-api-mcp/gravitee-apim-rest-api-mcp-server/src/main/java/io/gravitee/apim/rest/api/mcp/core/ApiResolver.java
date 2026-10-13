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
package io.gravitee.apim.rest.api.mcp.core;

import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.rest.api.mcp.core.model.ApiRef;
import io.gravitee.apim.rest.api.mcp.core.model.ApiSummary;
import io.gravitee.apim.rest.api.mcp.core.port.ApiDirectory;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Finds the API a caller names, by id or by name, among those the caller can see in one environment.
 */
public final class ApiResolver {

    private ApiResolver() {}

    /** @return null when no API is asked for */
    public static ApiRef resolve(ApiDirectory directory, AuditInfo caller, String api) {
        if (api == null || api.isBlank()) {
            return null;
        }
        var wanted = api.trim();
        var matches = find(directory.visibleTo(caller), wanted);
        if (matches.isEmpty()) {
            throw new TriageException("No API '" + wanted + "' is visible to you in this environment. Call list_apis to see them.");
        }
        if (matches.size() > 1) {
            var ids = matches.stream().map(ApiRef::id).collect(Collectors.joining(", "));
            throw new TriageException("Several APIs are named '" + wanted + "'. Pass one of these ids instead: " + ids + ".");
        }
        return matches.getFirst();
    }

    /** The APIs matching an id exactly, or failing that a name, case ignored. */
    public static List<ApiRef> find(List<ApiSummary> visible, String wanted) {
        var byId = visible
            .stream()
            .filter(candidate -> candidate.id().equals(wanted))
            .map(ApiSummary::ref)
            .toList();
        if (!byId.isEmpty()) {
            return byId;
        }
        return visible
            .stream()
            .filter(candidate -> candidate.name().equalsIgnoreCase(wanted))
            .map(ApiSummary::ref)
            .toList();
    }
}
