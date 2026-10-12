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
import io.gravitee.apim.rest.api.mcp.core.EnvironmentResolver;
import io.gravitee.apim.rest.api.mcp.core.model.ApiSummary;
import io.gravitee.apim.rest.api.mcp.core.model.EnvRef;
import io.gravitee.apim.rest.api.mcp.core.port.ApiDirectory;
import io.gravitee.apim.rest.api.mcp.core.port.EnvironmentDirectory;
import io.gravitee.apim.rest.api.mcp.spi.McpCaller;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The APIs the caller can see, with the handles the other tools take.
 */
@UseCase
public class ListApisUseCase {

    private final EnvironmentDirectory environments;
    private final ApiDirectory apis;

    public ListApisUseCase(EnvironmentDirectory environments, ApiDirectory apis) {
        this.environments = environments;
        this.apis = apis;
    }

    public record Input(McpCaller caller, String environment, String query) {}

    public record Output(String summary, List<Entry> apis) {}

    public record Entry(EnvRef environment, ApiSummary api) {}

    public Output execute(Input input) {
        var wanted = input.query() == null ? "" : input.query().trim().toLowerCase(Locale.ROOT);
        var entries = new ArrayList<Entry>();
        for (var env : EnvironmentResolver.environments(environments, input.caller(), input.environment())) {
            for (var api : apis.visibleTo(input.caller().on(env.id()))) {
                if (wanted.isEmpty() || api.name().toLowerCase(Locale.ROOT).contains(wanted)) {
                    entries.add(new Entry(env, api));
                }
            }
        }
        return new Output(entries.size() + " API(s)" + (wanted.isEmpty() ? "" : " named like '" + wanted + "'") + ".", entries);
    }
}
