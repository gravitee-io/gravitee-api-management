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
import io.gravitee.apim.rest.api.mcp.core.model.EnvRef;
import io.gravitee.apim.rest.api.mcp.core.port.ApiDirectory;
import io.gravitee.apim.rest.api.mcp.core.port.EnvironmentDirectory;
import io.gravitee.apim.rest.api.mcp.spi.McpCaller;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Decides which environment a question is about: the one named, the only one the caller can read, or the only one
 * where the API named exists.
 */
public final class EnvironmentResolver {

    private EnvironmentResolver() {}

    /** One environment, with the API when one is asked for. */
    public record Scope(EnvRef environment, AuditInfo auditInfo, ApiRef api) {}

    public static Scope resolve(EnvironmentDirectory environments, ApiDirectory apis, McpCaller caller, String environment, String api) {
        var readable = readable(environments, caller);
        if (environment != null && !environment.isBlank()) {
            var env = find(readable, environment.trim());
            var auditInfo = caller.on(env.id());
            return new Scope(env, auditInfo, ApiResolver.resolve(apis, auditInfo, api));
        }
        if (api != null && !api.isBlank()) {
            return byApi(readable, apis, caller, api.trim());
        }
        if (readable.size() == 1) {
            var env = readable.getFirst();
            return new Scope(env, caller.on(env.id()), null);
        }
        throw new TriageException("Pass environment: the id, hrid or name of one of " + labels(readable) + ".");
    }

    /** The environments a question without {@code environment} spans. */
    public static List<EnvRef> environments(EnvironmentDirectory environments, McpCaller caller, String environment) {
        var readable = readable(environments, caller);
        return environment == null || environment.isBlank() ? readable : List.of(find(readable, environment.trim()));
    }

    private static List<EnvRef> readable(EnvironmentDirectory environments, McpCaller caller) {
        var readable = environments.environments(caller).readable();
        if (readable.isEmpty()) {
            throw new TriageException("You are not allowed to read the analytics of any environment.");
        }
        return readable;
    }

    private static EnvRef find(List<EnvRef> readable, String wanted) {
        return readable
            .stream()
            .filter(env -> wanted.equals(env.id()) || wanted.equalsIgnoreCase(env.hrid()) || wanted.equalsIgnoreCase(env.name()))
            .findFirst()
            .orElseThrow(() -> new TriageException("No environment '" + wanted + "' among those you can read: " + labels(readable) + "."));
    }

    private static Scope byApi(List<EnvRef> readable, ApiDirectory apis, McpCaller caller, String api) {
        var found = new ArrayList<Scope>();
        for (var env : readable) {
            var auditInfo = caller.on(env.id());
            var matches = ApiResolver.find(apis.visibleTo(auditInfo), api);
            if (matches.size() == 1) {
                found.add(new Scope(env, auditInfo, matches.getFirst()));
            } else if (matches.size() > 1) {
                throw new TriageException(
                    "Several APIs are named '" +
                        api +
                        "' in " +
                        env.label() +
                        ". Pass environment and one of the ids: " +
                        matches.stream().map(ApiRef::id).collect(Collectors.joining(", ")) +
                        "."
                );
            }
        }
        if (found.isEmpty()) {
            throw new TriageException("No API '" + api + "' is visible to you in " + labels(readable) + ".");
        }
        if (found.size() > 1) {
            var envs = found
                .stream()
                .map(scope -> scope.environment().label())
                .collect(Collectors.joining(", "));
            throw new TriageException("Several environments have an API '" + api + "': " + envs + ". Pass environment.");
        }
        return found.getFirst();
    }

    private static String labels(List<EnvRef> environments) {
        return environments.stream().map(EnvRef::label).collect(Collectors.joining(", "));
    }
}
