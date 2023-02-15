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
import io.gravitee.rest.api.service.v4.EndpointConnectorPluginService;
import java.util.Set;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;

/**
 * Defines the REST resources to manage endpoints.
 *
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Path("/endpoints")
public class EndpointsResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private EndpointConnectorPluginService endpointService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Set<ConnectorPlugin> getEndpoints() {
        return ConnectorPluginMapper.INSTANCE.convertSet(endpointService.findAll());
    }

    @Path("/{endpointId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public ConnectorPlugin getEndpoint(@PathParam("endpointId") String endpointId) {
        return ConnectorPluginMapper.INSTANCE.convert(endpointService.findById(endpointId));
    }

    @GET
    @Path("/{endpointId}/schema")
    @Produces(MediaType.APPLICATION_JSON)
    public String getEndpointSchema(@PathParam("endpointId") String endpointId) {
        // Check that the endpoint exists
        endpointService.findById(endpointId);

        return endpointService.getSchema(endpointId);
    }

    @GET
    @Path("/{endpointId}/documentation")
    @Produces(MediaType.TEXT_PLAIN)
    public String getEndpointDoc(@PathParam("endpointId") String endpointId) {
        // Check that the endpoint exists
        endpointService.findById(endpointId);

        return endpointService.getDocumentation(endpointId);
    }
}
