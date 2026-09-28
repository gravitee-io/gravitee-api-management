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
package io.gravitee.gamma.infra.gravitee_plugin.resource;

import io.gravitee.common.http.MediaType;
import io.gravitee.gamma.core.port.service_provider.gravitee_plugin.AgentProviderPluginProvider;
import io.gravitee.gamma.infra.gravitee_plugin.resource.dto.PlatformPluginDto;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import java.util.Set;
import java.util.stream.Collectors;

public class AgentProviderPluginResource {

    @Inject
    private AgentProviderPluginProvider agentProviderPluginProvider;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Set<PlatformPluginDto> getAgentProviders() {
        return agentProviderPluginProvider.findAll().stream().map(PlatformPluginDto::from).collect(Collectors.toSet());
    }

    @GET
    @Path("/{pluginId}")
    @Produces(MediaType.APPLICATION_JSON)
    public PlatformPluginDto getAgentProvider(@PathParam("pluginId") String pluginId) {
        return PlatformPluginDto.from(agentProviderPluginProvider.findById(pluginId));
    }

    @GET
    @Path("/{pluginId}/schema")
    @Produces(MediaType.APPLICATION_JSON)
    public String getAgentProviderSchema(@PathParam("pluginId") String pluginId) {
        return agentProviderPluginProvider.getSchema(pluginId);
    }
}
