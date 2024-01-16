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
package io.gravitee.apim.core.plugin.use_case;

import io.gravitee.apim.core.plugin.model.ConnectorPlugin;
import io.gravitee.apim.core.plugin.query_service.EntrypointPluginQueryService;
import java.util.Set;

public class GetEntrypointPluginUseCase {

    private final EntrypointPluginQueryService entrypointPluginQueryService;

    public GetEntrypointPluginUseCase(EntrypointPluginQueryService entrypointPluginQueryService) {
        this.entrypointPluginQueryService = entrypointPluginQueryService;
    }

    public Output getEntrypointPluginsByOrganization(Input input) {
        return new Output(this.entrypointPluginQueryService.findByOrganization(input.organizationId));
    }

    public record Input(String organizationId) {}

    public record Output(Set<ConnectorPlugin> plugins) {}
}
