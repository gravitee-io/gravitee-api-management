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
package io.gravitee.rest.api.management.rest.resource;

import io.gravitee.common.data.domain.Page;
import io.gravitee.common.http.MediaType;
import io.gravitee.repository.management.model.Event;
import io.gravitee.rest.api.management.rest.model.wrapper.EventEntityPage;
import io.gravitee.rest.api.management.rest.resource.param.EventSearchParam;
import io.gravitee.rest.api.management.rest.resource.param.EventTypeListParam;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.EventEntity;
import io.gravitee.rest.api.model.EventQuery;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.service.EventService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at gravitee.io)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "API Events")
public class ApiEventsResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private EventService eventService;

    @SuppressWarnings("UnresolvedRestParam")
    @PathParam("api")
    @Parameter(name = "api", hidden = true)
    private String api;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get API's events", description = "User must have the MANAGE_LIFECYCLE permission to use this service")
    @ApiResponse(
        responseCode = "200",
        description = "API's events",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            array = @ArraySchema(schema = @Schema(implementation = EventEntity.class))
        )
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.API_EVENT, acls = RolePermissionAction.READ) })
    public List<EventEntity> getApiEventsEvents(
        @Parameter(explode = Explode.FALSE, schema = @Schema(type = "array")) @QueryParam("type") EventTypeListParam eventTypeListParam
    ) {
        final EventQuery query = new EventQuery();
        query.setApi(api);

        ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        if (executionContext.hasEnvironmentId()) {
            query.setEnvironmentIds(List.of(executionContext.getEnvironmentId()));
        }
        return eventService
            .search(executionContext, query)
            .stream()
            .filter(event -> eventTypeListParam.contains(event.getType()))
            .sorted((e1, e2) -> e2.getCreatedAt().compareTo(e1.getCreatedAt()))
            .collect(Collectors.toList());
    }

    @Path("{eventId}")
    public ApiEventResource getApiEventResource() {
        return resourceContext.getResource(ApiEventResource.class);
    }

    @GET
    @Path("search")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get API's events", description = "User must have the API_EVENT[READ] permission to use this service")
    @ApiResponse(
        responseCode = "200",
        description = "Page of API events",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EventEntityPage.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.API_EVENT, acls = RolePermissionAction.READ) })
    public EventEntityPage searchApiEvents(@Parameter @BeanParam EventSearchParam eventSearchParam) {
        ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        GenericApiEntity genericApi = apiSearchService.findGenericById(executionContext, api);

        Map<String, Object> properties = new HashMap<>();
        properties.put(Event.EventProperties.API_ID.getValue(), List.of(api));
        final Page<EventEntity> apiEvents = eventService.search(
            executionContext,
            eventSearchParam.getEventTypeListParam(),
            properties,
            eventSearchParam.getFrom(),
            eventSearchParam.getTo(),
            eventSearchParam.getPage(),
            eventSearchParam.getSize(),
            Collections.singletonList(GraviteeContext.getCurrentEnvironment())
        );

        apiEvents
            .getContent()
            .forEach(event -> {
                Map<String, String> properties1 = event.getProperties();
                if (!eventSearchParam.isWithPayload()) {
                    // Remove payload content from response since it's not required anymore
                    event.setPayload(null);
                }
                // complete event with API info
                properties1.put("api_name", genericApi.getName());
                properties1.put("api_version", genericApi.getApiVersion());
            });

        return new EventEntityPage(apiEvents);
    }
}
