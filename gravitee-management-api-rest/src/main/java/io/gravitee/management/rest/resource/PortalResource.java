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
package io.gravitee.management.rest.resource;

import io.gravitee.common.http.MediaType;
import io.gravitee.management.model.PortalConfigEntity;
import io.gravitee.management.model.permissions.RolePermission;
import io.gravitee.management.rest.resource.portal.PortalApisResource;
import io.gravitee.management.rest.security.Permission;
import io.gravitee.management.rest.security.Permissions;
import io.gravitee.management.service.ConfigService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import static io.gravitee.management.model.permissions.RolePermissionAction.*;

/**
 * Defines the REST resources to manage Portal.
 *
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Path("/portal")
@Api(tags = {"Portal"})
public class PortalResource {

    @Inject
    private ConfigService configService;

    @Context
    private ResourceContext resourceContext;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public PortalConfigEntity getConfig() {
        return configService.getPortalConfig();
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Save the portal configuration")
    @Permissions({
            @Permission(value = RolePermission.PORTAL_SETTINGS, acls = {CREATE, UPDATE, DELETE})
    })
    public Response savePortalConfig(
            @ApiParam(name = "config", required = true) @NotNull PortalConfigEntity portalConfigEntity) {
        configService.save(portalConfigEntity);
        return Response
                .ok()
                .entity(portalConfigEntity)
                .build();
    }

    @Path("pages")
    public PortalPagesResource getPortalPagesResource() {
        return resourceContext.getResource(PortalPagesResource.class);
    }

    @Path("apis")
    public PortalApisResource getPortalApisResource() {
        return resourceContext.getResource(PortalApisResource.class);
    }
}
