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
package io.gravitee.plugin.console;

import io.gravitee.common.http.MediaType;
import io.gravitee.plugin.console.internal.ConsoleExtensionService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import java.util.List;

/**
 * REST resource for dynamically loaded console extensions.
 *
 * @author GraviteeSource Team
 */
@Path("")
public class ConsoleExtensionsResource {

    @Inject
    private ConsoleExtensionService consoleExtensionService;

    @Context
    private ResourceContext resourceContext;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<ConsoleExtensionEntity> list() {
        return consoleExtensionService.list();
    }

    @GET
    @Path("/{pluginId}/assets/{path:.+}")
    public Response getAsset(@PathParam("pluginId") String pluginId, @PathParam("path") String path) {
        return consoleExtensionService.getAsset(pluginId, path);
    }

    @Path("/{pluginId}")
    public Object getPluginResource(@PathParam("pluginId") String pluginId) {
        Class<?> resourceClass = consoleExtensionService.getResourceClass(pluginId);
        if (resourceClass == null) {
            throw new NotFoundException();
        }
        return resourceContext.getResource(resourceClass);
    }

    @Path("/environments/{envId}/{pluginId}")
    public Object getPluginResourceWithContext(@PathParam("pluginId") String pluginId) {
        Class<?> resourceClass = consoleExtensionService.getResourceClass(pluginId);
        if (resourceClass == null) {
            throw new NotFoundException();
        }
        return resourceContext.getResource(resourceClass);
    }
}
