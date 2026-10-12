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
package io.gravitee.apim.rest.api.mcp.core.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.rest.api.mcp.core.model.EnvRef;
import io.gravitee.apim.rest.api.mcp.core.port.ApiDirectory;
import io.gravitee.apim.rest.api.mcp.core.port.EnvironmentDirectory;
import io.gravitee.apim.rest.api.mcp.spi.McpCaller;
import java.util.ArrayList;
import java.util.List;

/**
 * The environments of the caller's organization, so that a question can name one.
 */
@UseCase
public class ListEnvironmentsUseCase {

    private final EnvironmentDirectory environments;
    private final ApiDirectory apis;

    public ListEnvironmentsUseCase(EnvironmentDirectory environments, ApiDirectory apis) {
        this.environments = environments;
        this.apis = apis;
    }

    public record Input(McpCaller caller) {}

    public record Output(String summary, List<Environment> environments) {}

    /**
     * @param readable whether the caller may read the traffic of the environment; the other tools cover it only then
     * @param visibleApis how many APIs of the environment the caller can see; null when the environment is not readable
     */
    public record Environment(String id, String hrid, String name, boolean readable, Integer visibleApis) {}

    public Output execute(Input input) {
        var caller = input.caller();
        var directory = environments.environments(caller);
        var listed = new ArrayList<Environment>();
        for (var env : directory.readable()) {
            listed.add(new Environment(env.id(), env.hrid(), env.name(), true, apis.visibleTo(caller.on(env.id())).size()));
        }
        for (var env : directory.unreadable()) {
            listed.add(new Environment(env.id(), env.hrid(), env.name(), false, null));
        }
        var summary = directory.readable().size() + " environment(s) you can read, " + directory.unreadable().size() + " you cannot.";
        if (directory.readable().size() == 1) {
            summary += " Pass environment only when a tool asks for it.";
        } else if (!directory.readable().isEmpty()) {
            summary += " Pass environment (id, hrid or name) to the tools that work on one.";
        }
        return new Output(summary, listed);
    }
}
