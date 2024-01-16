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

import io.gravitee.apim.core.plugin.use_case.GetEntrypointPluginsUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.mapper.ConnectorPluginMapper;
import io.gravitee.rest.api.management.v2.rest.mapper.MoreInformationMapper;
import io.gravitee.rest.api.management.v2.rest.model.ConnectorPlugin;
import io.gravitee.rest.api.management.v2.rest.model.MoreInformation;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.model.platform.plugin.SchemaDisplayFormat;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.v4.EntrypointConnectorPluginService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import java.util.Set;

/**
 * Defines the REST resources to manage entrypoints.
 *
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Path("/plugins/entrypoints")
public class EntrypointsResource extends AbstractResource {

    @Inject
    private EntrypointConnectorPluginService entrypointService;

    @Inject
    private GetEntrypointPluginsUseCase getEntrypointPluginsUseCase;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Set<ConnectorPlugin> getEntrypoints() {
        String organizationId = GraviteeContext.getCurrentOrganization() != null
            ? GraviteeContext.getCurrentOrganization()
            : GraviteeContext.getDefaultOrganization();
        return ConnectorPluginMapper.INSTANCE.mapCorePlugin(
            getEntrypointPluginsUseCase.getEntrypointPluginsByOrganization(new GetEntrypointPluginsUseCase.Input(organizationId)).plugins()
        );
    }

    @Path("/{entrypointId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public ConnectorPlugin getEntrypoint(@PathParam("entrypointId") String entrypointId) {
        return ConnectorPluginMapper.INSTANCE.map(entrypointService.findById(entrypointId));
    }

    @GET
    @Path("/{entrypointId}/schema")
    @Produces(MediaType.APPLICATION_JSON)
    public String getEntrypointSchema(@PathParam("entrypointId") String entrypointId) {
        // Check that the entrypoint exists
        entrypointService.findById(entrypointId);

        return entrypointService.getSchema(entrypointId);
    }

    @GET
    @Path("/{entrypointId}/documentation")
    @Produces(MediaType.TEXT_PLAIN)
    public String getEntrypointDocumentation(@PathParam("entrypointId") String entrypointId) {
        // Check that the entrypoint exists
        entrypointService.findById(entrypointId);

        return entrypointService.getDocumentation(entrypointId);
    }

    @GET
    @Path("/{entrypointId}/subscription-schema")
    @Produces(MediaType.APPLICATION_JSON)
    public String getEntrypointSubscriptionSchema(@PathParam("entrypointId") String entrypointId, @QueryParam("display") String display) {
        // Check that the entrypoint exists
        entrypointService.findById(entrypointId);
        if (display != null) {
            return entrypointService.getSubscriptionSchema(entrypointId, SchemaDisplayFormat.fromLabel(display));
        }
        return entrypointService.getSubscriptionSchema(entrypointId);
    }

    @GET
    @Path("/{entrypointId}/more-information")
    @Produces(MediaType.APPLICATION_JSON)
    public MoreInformation getEntrypointMoreInformation(@PathParam("entrypointId") String entrypointId) {
        // Check that the entrypoint exists
        entrypointService.findById(entrypointId);

        return MoreInformationMapper.INSTANCE.map(entrypointService.getMoreInformation(entrypointId));
    }
}
