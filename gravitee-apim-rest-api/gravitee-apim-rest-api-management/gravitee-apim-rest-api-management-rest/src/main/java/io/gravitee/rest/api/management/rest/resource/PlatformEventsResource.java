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

import static io.gravitee.rest.api.model.permissions.RolePermission.API_ANALYTICS;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.READ;

import io.gravitee.common.data.domain.Page;
import io.gravitee.common.http.MediaType;
import io.gravitee.repository.management.model.Event;
import io.gravitee.rest.api.management.rest.model.wrapper.EventEntityPage;
import io.gravitee.rest.api.management.rest.resource.param.EventSearchParam;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.EventEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.EventService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "Platform Events")
public class PlatformEventsResource extends AbstractResource {

    @Inject
    private EventService eventService;

    @Inject
    private ApiService apiService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "List platform events",
        description = "User must have the MANAGEMENT_PLATFORM[READ] permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Platform events",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EventEntityPage.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_PLATFORM, acls = READ) })
    public EventEntityPage getPlatformEvents(@BeanParam EventSearchParam eventSearchParam) {
        eventSearchParam.validate();

        Map<String, Object> properties = new HashMap<>();
        if (eventSearchParam.getApiIdsParam() != null && !eventSearchParam.getApiIdsParam().isEmpty()) {
            properties.put(Event.EventProperties.API_ID.getValue(), eventSearchParam.getApiIdsParam());
        } else if (!isAdmin()) {
            properties.put(
                Event.EventProperties.API_ID.getValue(),
                apiService
                    .findByUser(getAuthenticatedUser(), null, false)
                    .stream()
                    .filter(api -> permissionService.hasPermission(API_ANALYTICS, api.getId(), READ))
                    .map(ApiEntity::getId)
                    .collect(Collectors.joining(","))
            );
        }

        Page<EventEntity> events = eventService.search(
            eventSearchParam.getEventTypeListParam(),
            properties,
            eventSearchParam.getFrom(),
            eventSearchParam.getTo(),
            eventSearchParam.getPage(),
            eventSearchParam.getSize(),
            Collections.singletonList(GraviteeContext.getCurrentEnvironment())
        );

        events
            .getContent()
            .forEach(
                event -> {
                    Map<String, String> properties1 = event.getProperties();
                    // Event relative to API
                    if (properties1 != null && properties1.containsKey(Event.EventProperties.API_ID.getValue())) {
                        // Remove payload content from response since it's not required anymore
                        event.setPayload(null);

                        // Retrieve additional data
                        String apiId = properties1.get(Event.EventProperties.API_ID.getValue());
                        try {
                            ApiEntity api = apiService.findById(apiId);
                            properties1.put("api_name", api.getName());
                            properties1.put("api_version", api.getVersion());
                        } catch (ApiNotFoundException anfe) {
                            properties1.put("deleted", Boolean.TRUE.toString());
                            properties1.put("api_name", "Deleted API");
                        }
                    }
                }
            );

        return new EventEntityPage(events);
    }
}
