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
import io.gravitee.rest.api.management.rest.model.PagedResult;
import io.gravitee.rest.api.model.notification.PortalNotificationEntity;
import io.gravitee.rest.api.service.PortalNotificationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = {"User Notifications"})
public class UserNotificationsResource extends AbstractResource  {

    @Autowired
    private PortalNotificationService portalNotificationService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "List user's notifications")
    @ApiResponses({
            @ApiResponse(code = 200, message = "User's notifications"),
            @ApiResponse(code = 404, message = "User not found"),
            @ApiResponse(code = 500, message = "Internal server error")})
    public PagedResult<PortalNotificationEntity> getUserNotifications()  {
        List<PortalNotificationEntity> notifications = portalNotificationService.findByUser(getAuthenticatedUser())
                .stream()
                .sorted(Comparator.comparing(PortalNotificationEntity::getCreatedAt))
                .collect(Collectors.toList());

        return new PagedResult<>(notifications);
    }

    @DELETE
    @ApiOperation(value = "Delete all user's notifications")
    @ApiResponses({
            @ApiResponse(code = 204, message = "Notifications successfully deleted"),
            @ApiResponse(code = 404, message = "User not found"),
            @ApiResponse(code = 500, message = "Internal server error")})
    public Response deleteAllUserNotifications() {
        portalNotificationService.deleteAll(getAuthenticatedUser());
        return Response
                .status(Response.Status.NO_CONTENT)
                .build();
    }

    @Path("{notification}")
    @DELETE
    @ApiOperation(value = "Delete a single user's notification")
    @ApiResponses({
            @ApiResponse(code = 204, message = "Notification successfully deleted"),
            @ApiResponse(code = 404, message = "User not found"),
            @ApiResponse(code = 500, message = "Internal server error")})
    public Response deleteUserNotification(@PathParam("notification") String notificationId) {
        portalNotificationService.delete(notificationId);
        return Response
                .status(Response.Status.NO_CONTENT)
                .build();
    }
}
