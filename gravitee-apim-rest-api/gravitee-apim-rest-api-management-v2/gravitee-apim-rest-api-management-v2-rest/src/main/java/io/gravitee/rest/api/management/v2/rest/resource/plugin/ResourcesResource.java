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
package io.gravitee.rest.api.management.v2.rest.resource.plugin;

import io.gravitee.apim.core.plugin.use_case.GetResourcePluginUseCase;
import io.gravitee.apim.core.plugin.use_case.GetResourcePluginsUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.mapper.ResourcePluginMapper;
import io.gravitee.rest.api.management.v2.rest.model.ResourcePlugin;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import java.util.Set;

@Path("/plugins/resources")
public class ResourcesResource extends AbstractResource {

    @Inject
    private GetResourcePluginsUseCase getResourcePluginsUseCase;

    @Inject
    private GetResourcePluginUseCase getResourcePluginUseCase;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Set<ResourcePlugin> getResources() {
        return ResourcePluginMapper.INSTANCE.mapFromCore(
            getResourcePluginsUseCase
                .getResourcesByOrganization(new GetResourcePluginsUseCase.Input(GraviteeContext.getCurrentOrganization()))
                .plugins()
        );
    }

    @Path("/{resourceId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public ResourcePlugin getResource(@PathParam("resourceId") String resourceId) {
        return ResourcePluginMapper.INSTANCE.mapFromCore(
            getResourcePluginUseCase
                .execute(new GetResourcePluginUseCase.Input(GraviteeContext.getCurrentOrganization(), resourceId))
                .plugin()
        );
    }

    @GET
    @Path("/{resourceId}/schema")
    @Produces(MediaType.APPLICATION_JSON)
    public String getResourceSchema(@PathParam("resourceId") String resourceId) {
        return getResourcePluginUseCase
            .execute(new GetResourcePluginUseCase.Input(GraviteeContext.getCurrentOrganization(), resourceId, true))
            .schema();
    }
}
