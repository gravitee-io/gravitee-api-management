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
import io.gravitee.rest.api.management.rest.model.wrapper.PortalNotificationPageResult;
import io.gravitee.rest.api.management.rest.resource.AbstractResource;
import io.gravitee.rest.api.model.notification.PortalNotificationEntity;
import io.gravitee.rest.api.service.PortalNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "User Notifications")
public class UserNotificationsResource extends AbstractResource {

    @Autowired
    private PortalNotificationService portalNotificationService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "List user's notifications")
    @ApiResponse(
        responseCode = "200",
        description = "User's notifications",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PortalNotificationPageResult.class))
    )
    @ApiResponse(responseCode = "404", description = "User not found")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public PortalNotificationPageResult getUserNotifications() {
        List<PortalNotificationEntity> notifications = portalNotificationService
            .findByUser(getAuthenticatedUser())
            .stream()
            .sorted(Comparator.comparing(PortalNotificationEntity::getCreatedAt))
            .collect(Collectors.toList());

        return new PortalNotificationPageResult(notifications);
    }

    @DELETE
    @Operation(summary = "Delete all user's notifications")
    @ApiResponse(responseCode = "204", description = "Notifications successfully deleted")
    @ApiResponse(responseCode = "404", description = "User not found")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public Response deleteAllUserNotifications() {
        portalNotificationService.deleteAll(getAuthenticatedUser());
        return Response.status(Response.Status.NO_CONTENT).build();
    }

    @Path("{notification}")
    @DELETE
    @Operation(summary = "Delete a single user's notification")
    @ApiResponse(responseCode = "204", description = "Notification successfully deleted")
    @ApiResponse(responseCode = "404", description = "User not found")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public Response deleteUserNotification(@PathParam("notification") String notificationId) {
        portalNotificationService.delete(notificationId);
        return Response.status(Response.Status.NO_CONTENT).build();
    }
}
