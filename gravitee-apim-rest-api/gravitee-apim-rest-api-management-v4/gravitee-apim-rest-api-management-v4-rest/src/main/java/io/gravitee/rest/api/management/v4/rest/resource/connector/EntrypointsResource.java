/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.management.v4.rest.resource.connector;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v4.rest.mapper.ConnectorPluginMapper;
import io.gravitee.rest.api.management.v4.rest.model.ConnectorPlugin;
import io.gravitee.rest.api.model.v4.connector.ConnectorPluginEntity;
import io.gravitee.rest.api.service.v4.EntrypointConnectorPluginService;
import java.util.Set;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;

/**
 * Defines the REST resources to manage entrypoints.
 *
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Path("/entrypoints")
public class EntrypointsResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private EntrypointConnectorPluginService entrypointService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Set<ConnectorPlugin> getEntrypoints() {
        return ConnectorPluginMapper.INSTANCE.convertSet(entrypointService.findAll());
    }

    @Path("/{entrypointId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public ConnectorPlugin getEntrypoint(@PathParam("entrypointId") String entrypointId) {
        return ConnectorPluginMapper.INSTANCE.convert(entrypointService.findById(entrypointId));
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
    public String getEntrypointDoc(@PathParam("entrypointId") String entrypointId) {
        // Check that the entrypoint exists
        entrypointService.findById(entrypointId);

        return entrypointService.getDocumentation(entrypointId);
    }

    @GET
    @Path("/{entrypointId}/subscriptionSchema")
    @Produces(MediaType.APPLICATION_JSON)
    public String getEntrypointSubscriptionSchema(@PathParam("entrypointId") String entrypointId) {
        // Check that the entrypoint exists
        entrypointService.findById(entrypointId);

        return entrypointService.getSubscriptionSchema(entrypointId);
    }
}
