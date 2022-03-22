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
package io.gravitee.rest.api.management.rest.resource;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.rest.model.OrganizationFlowConfiguration;
import io.gravitee.rest.api.service.OrganizationService;
import io.gravitee.rest.api.service.configuration.flow.FlowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "Flows")
public class FlowsResource extends AbstractResource {

    @Inject
    private FlowService flowService;

    @Inject
    private OrganizationService organizationService;

    @SuppressWarnings("UnresolvedRestParam")
    @PathParam("orgId")
    @Parameter(name = "orgId", hidden = true)
    private String orgId;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get the global flow configuration of the organization")
    @ApiResponse(
        responseCode = "200",
        description = "Platform flows configuration",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = OrganizationFlowConfiguration.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public Response hasPolicies() {
        boolean hasPlatformPolicies = !organizationService.findById(orgId).getFlows().isEmpty();

        OrganizationFlowConfiguration flowConfiguration = new OrganizationFlowConfiguration();
        flowConfiguration.setHasPolicies(hasPlatformPolicies);

        return Response.ok(flowConfiguration).build();
    }

    @GET
    @Path("configuration-schema")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getConfigurationSchemaForm() {
        return Response.ok(flowService.getConfigurationSchemaForm()).build();
    }

    @GET
    @Path("flow-schema")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPlatformFlowSchemaForm() {
        return Response.ok(flowService.getPlatformFlowSchemaForm()).build();
    }
}
