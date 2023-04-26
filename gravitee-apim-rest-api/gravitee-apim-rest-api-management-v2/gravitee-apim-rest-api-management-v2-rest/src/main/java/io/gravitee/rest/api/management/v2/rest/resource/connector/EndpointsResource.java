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
package io.gravitee.rest.api.management.v2.rest.resource.connector;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.mapper.ConnectorPluginMapper;
import io.gravitee.rest.api.management.v2.rest.mapper.MoreInformationMapper;
import io.gravitee.rest.api.management.v2.rest.model.ConnectorPlugin;
import io.gravitee.rest.api.management.v2.rest.model.EndpointsResponse;
import io.gravitee.rest.api.management.v2.rest.model.MoreInformation;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.v2.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.service.v4.EndpointConnectorPluginService;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;

/**
 * Defines the REST resources to manage endpoints.
 *
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Path("/plugins/endpoints")
public class EndpointsResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private EndpointConnectorPluginService endpointService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public EndpointsResponse getEndpoints(@BeanParam @Valid PaginationParam paginationParam) {
        Set<ConnectorPlugin> connectorPlugins = ConnectorPluginMapper.INSTANCE.convertSet(endpointService.findAll());
        List paginationData = computePaginationData(connectorPlugins, paginationParam);
        return new EndpointsResponse()
            .data(paginationData)
            .pagination(computePaginationInfo(connectorPlugins.size(), paginationData.size(), paginationParam))
            .links(computePaginationLinks(connectorPlugins.size(), paginationParam));
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
    public String getEndpointDocumentation(@PathParam("endpointId") String endpointId) {
        // Check that the endpoint exists
        endpointService.findById(endpointId);

        return endpointService.getDocumentation(endpointId);
    }

    @GET
    @Path("/{endpointId}/more-information")
    @Produces(MediaType.APPLICATION_JSON)
    public MoreInformation getEndpointMoreInformation(@PathParam("endpointId") String endpointId) {
        // Check that the entrypoint exists
        endpointService.findById(endpointId);

        return MoreInformationMapper.INSTANCE.convert(endpointService.getMoreInformation(endpointId));
    }

    @GET
    @Path("/{endpointId}/shared-configuration-schema")
    @Produces(MediaType.APPLICATION_JSON)
    public String getEndpointSharedConfigurationSchema(@PathParam("endpointId") String endpointId) {
        // Check that the entrypoint exists
        endpointService.findById(endpointId);

        return endpointService.getSharedConfigurationSchema(endpointId);
    }
}
