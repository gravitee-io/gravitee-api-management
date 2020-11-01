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
import io.gravitee.rest.api.management.rest.resource.param.EventTypeListParam;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.EventEntity;
import io.gravitee.rest.api.model.EventQuery;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.EventService;
import io.swagger.annotations.*;

import javax.inject.Inject;
import javax.ws.rs.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at gravitee.io)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = {"API Events"})
public class ApiEventsResource extends AbstractResource {

    @Inject
    private EventService eventService;

    @SuppressWarnings("UnresolvedRestParam")
    @PathParam("api")
    @ApiParam(name = "api", hidden = true)
    private String api;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get API's events",
            notes = "User must have the MANAGE_LIFECYCLE permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "API's events"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.API_EVENT, acls = RolePermissionAction.READ)
    })
    public List<EventEntity> getApiEventsEvents(@ApiParam @DefaultValue("all") @QueryParam("type") EventTypeListParam eventTypeListParam) {
        final EventQuery query = new EventQuery();
        query.setApi(api);
        return eventService.search(query).stream()
                .filter(event -> eventTypeListParam.getEventTypes().contains(event.getType()))
                .sorted((e1, e2) -> e2.getCreatedAt().compareTo(e1.getCreatedAt()))
                .collect(Collectors.toList());
    }

}
