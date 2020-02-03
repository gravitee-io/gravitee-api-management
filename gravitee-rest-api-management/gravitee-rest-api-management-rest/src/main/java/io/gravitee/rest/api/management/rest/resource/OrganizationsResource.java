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
import io.gravitee.rest.api.model.NewOrganizationEntity;
import io.gravitee.rest.api.service.OrganizationService;
import io.swagger.annotations.*;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Path("/organizations")
@Api
public class OrganizationsResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    OrganizationService organizationService;

    /**
     * Create a new Organization for the authenticated user.
     * 
     * @param newOrganizationEntity
     * @return
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Create an Organization", tags = {"Organization"})
    @ApiResponses({ @ApiResponse(code = 201, message = "Organization successfully created"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public Response createOrganization(
            @ApiParam(name = "newOrganizationEntity", required = true) @Valid @NotNull final NewOrganizationEntity newOrganizationEntity) {
        return Response
                .status(Status.CREATED)
                .entity(organizationService.create(newOrganizationEntity))
                .build();
    }

    @Path("/{orgId}")
    public OrganizationResource getOrganizationResource() {
        return resourceContext.getResource(OrganizationResource.class);
    }
}
