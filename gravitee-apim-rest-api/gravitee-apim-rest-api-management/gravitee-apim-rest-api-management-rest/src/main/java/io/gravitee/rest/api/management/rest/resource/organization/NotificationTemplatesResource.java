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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Notifications")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class NotificationTemplatesResource extends AbstractResource {

    @Inject
    private NotificationTemplateService notificationTemplateService;

    @GET
    @Operation(
        summary = "List all notification templates.",
        description = "User must have the NOTIFICATION_TEMPLATES[READ] permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Notifications templates",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            array = @ArraySchema(schema = @Schema(implementation = NotificationTemplateEntity.class))
        )
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.ORGANIZATION_NOTIFICATION_TEMPLATES, acls = RolePermissionAction.READ) })
    public Response getNotificationTemplates(
        @Parameter(description = "filter by notification scope") @QueryParam("scope") String scope,
        @Parameter(description = "filter by notification hook") @QueryParam("hook") String hook
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
    @Operation(
        summary = "Create a notification template",
        description = "User must have the NOTIFICATION_TEMPLATES[CREATE] permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Created notification template",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = NotificationTemplateEntity.class))
    )
    @ApiResponse(responseCode = "400", description = "There must not be any ID in the payload")
    @ApiResponse(responseCode = "500", description = "Internal server error")
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
    @Operation(
        summary = "Get a specific notification template.",
        description = "User must have the NOTIFICATION_TEMPLATES[READ] permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Notification template found",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = NotificationTemplateEntity.class))
    )
    @ApiResponse(responseCode = "404", description = "Notification template not found")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.ORGANIZATION_NOTIFICATION_TEMPLATES, acls = RolePermissionAction.READ) })
    public Response getNotificationTemplate(
        @Parameter(description = "ID of the notification template") @PathParam("notificationTemplateId") String notificationTemplateId
    ) {
        final NotificationTemplateEntity notificationTemplateEntity = notificationTemplateService.findById(notificationTemplateId);
        return Response.ok(notificationTemplateEntity).build();
    }

    @Path("{notificationTemplateId}")
    @PUT
    @Operation(
        summary = "Update an existing notification template",
        description = "User must have the NOTIFICATION_TEMPLATES[UPDATE] permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Updated notification template",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = NotificationTemplateEntity.class))
    )
    @ApiResponse(responseCode = "400", description = "ID in path parameter is not the same as in the payload")
    @ApiResponse(responseCode = "404", description = "Notification template not found")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.ORGANIZATION_NOTIFICATION_TEMPLATES, acls = RolePermissionAction.UPDATE) })
    public Response updateNotificationTemplate(
        @Parameter(description = "ID of the notification template") @PathParam("notificationTemplateId") String notificationTemplateId,
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
