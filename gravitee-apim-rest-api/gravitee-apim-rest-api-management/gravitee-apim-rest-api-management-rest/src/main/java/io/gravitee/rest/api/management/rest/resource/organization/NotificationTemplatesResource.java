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

import io.gravitee.rest.api.management.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.notification.NotificationTemplateEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.notification.NotificationTemplateService;
import io.swagger.annotations.*;
import java.util.Set;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = { "Notifications" })
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class NotificationTemplatesResource extends AbstractResource {

    @Inject
    private NotificationTemplateService notificationTemplateService;

    @GET
    @ApiOperation(
        value = "List all notification templates.",
        notes = "User must have the NOTIFICATION_TEMPLATES[READ] permission to use this service"
    )
    @ApiResponses(
        {
            @ApiResponse(
                code = 200,
                message = "Notifications templates",
                responseContainer = "List",
                response = NotificationTemplateEntity.class
            ),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    @Permissions({ @Permission(value = RolePermission.ORGANIZATION_NOTIFICATION_TEMPLATES, acls = RolePermissionAction.READ) })
    public Response getNotificationTemplates(
        @ApiParam(value = "filter by notification scope") @QueryParam("scope") String scope,
        @ApiParam(value = "filter by notification hook") @QueryParam("hook") String hook
    ) {
        if (hook == null || scope == null) {
            final Set<NotificationTemplateEntity> all = notificationTemplateService.findAll();
            return Response.ok(all).build();
        } else {
            final Set<NotificationTemplateEntity> allByHookAndScope = notificationTemplateService.findByHookAndScope(hook, scope);
            return Response.ok(allByHookAndScope).build();
        }
    }

    @POST
    @ApiOperation(
        value = "Create a notification template",
        notes = "User must have the NOTIFICATION_TEMPLATES[CREATE] permission to use this service"
    )
    @ApiResponses(
        {
            @ApiResponse(code = 200, message = "Created notification template", response = NotificationTemplateEntity.class),
            @ApiResponse(code = 400, message = "There must not be any ID in the payload"),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    @Permissions({ @Permission(value = RolePermission.ORGANIZATION_NOTIFICATION_TEMPLATES, acls = RolePermissionAction.CREATE) })
    public Response createNotificationTemplate(@Valid NotificationTemplateEntity newNotificationTemplateEntity) {
        final NotificationTemplateEntity createdNotificationTemplate = notificationTemplateService.create(newNotificationTemplateEntity);
        if (createdNotificationTemplate != null) {
            return Response.ok(createdNotificationTemplate).build();
        }
        return Response.serverError().build();
    }

    @Path("{notificationTemplateId}")
    @GET
    @ApiOperation(
        value = "Get a specific notification template.",
        notes = "User must have the NOTIFICATION_TEMPLATES[READ] permission to use this service"
    )
    @ApiResponses(
        {
            @ApiResponse(code = 200, message = "Notification template found", response = NotificationTemplateEntity.class),
            @ApiResponse(code = 404, message = "Notification template not found"),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    @Permissions({ @Permission(value = RolePermission.ORGANIZATION_NOTIFICATION_TEMPLATES, acls = RolePermissionAction.READ) })
    public Response getNotificationTemplate(
        @ApiParam(value = "ID of the notification template") @PathParam("notificationTemplateId") String notificationTemplateId
    ) {
        final NotificationTemplateEntity notificationTemplateEntity = notificationTemplateService.findById(notificationTemplateId);
        return Response.ok(notificationTemplateEntity).build();
    }

    @Path("{notificationTemplateId}")
    @PUT
    @ApiOperation(
        value = "Update an existing notification template",
        notes = "User must have the NOTIFICATION_TEMPLATES[UPDATE] permission to use this service"
    )
    @ApiResponses(
        {
            @ApiResponse(code = 200, message = "Updated notification template", response = NotificationTemplateEntity.class),
            @ApiResponse(code = 400, message = "ID in path parameter is not the same as in the payload"),
            @ApiResponse(code = 404, message = "Notification template not found"),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    @Permissions({ @Permission(value = RolePermission.ORGANIZATION_NOTIFICATION_TEMPLATES, acls = RolePermissionAction.UPDATE) })
    public Response updateNotificationTemplate(
        @ApiParam(value = "ID of the notification template") @PathParam("notificationTemplateId") String notificationTemplateId,
        @Valid NotificationTemplateEntity notificationTemplateEntityUpdate
    ) {
        if (!notificationTemplateEntityUpdate.getId().equals(notificationTemplateId)) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        final NotificationTemplateEntity updatedNotificationTemplate = notificationTemplateService.update(notificationTemplateEntityUpdate);
        if (updatedNotificationTemplate != null) {
            return Response.ok(updatedNotificationTemplate).build();
        }
        return Response.serverError().build();
    }
}
