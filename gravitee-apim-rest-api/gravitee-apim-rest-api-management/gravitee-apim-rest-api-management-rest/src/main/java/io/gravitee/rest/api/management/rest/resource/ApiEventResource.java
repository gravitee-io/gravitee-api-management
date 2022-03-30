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
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.EventEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.EventService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

@Tag(name = "API Events")
public class ApiEventResource extends AbstractResource {

    @Inject
    private EventService eventService;

    @PathParam("api")
    @Parameter(name = "api", hidden = true)
    private String api;

    @PathParam("eventId")
    @Parameter(name = "eventId")
    private String eventId;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get an API event with its id", description = "User must have the READ API_EVENT permission to use this service")
    @ApiResponse(
        responseCode = "200",
        description = "API event",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EventEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.API_EVENT, acls = RolePermissionAction.READ) })
    public Response getEvent() {
        return Response.ok(eventService.findById(GraviteeContext.getExecutionContext(), eventId)).build();
    }
}
