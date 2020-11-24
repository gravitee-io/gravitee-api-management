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
package io.gravitee.rest.api.management.rest.resource.organization;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.parameters.ConsoleConfigEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.service.ConfigService;
import io.swagger.annotations.*;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import static io.gravitee.rest.api.model.permissions.RolePermissionAction.*;

/**
 * Defines the REST resources to manage Portal.
 *
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = {"Console"})
public class ConsoleResource {

    @Inject
    private ConfigService configService;

    @Context
    private ResourceContext resourceContext;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public ConsoleConfigEntity getConsoleConfig() {
        return configService.getConsoleConfig();
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Save the management configuration")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Console configuration", response = ConsoleConfigEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @Permissions({
            @Permission(value = RolePermission.ORGANIZATION_SETTINGS, acls = {CREATE, UPDATE, DELETE})
    })
    public Response saveConsoleConfig(
            @ApiParam(name = "config", required = true) @NotNull ConsoleConfigEntity consoleConfigEntity) {
        configService.save(consoleConfigEntity);
        return Response
                .ok()
                .entity(consoleConfigEntity)
                .build();
    }

}
