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
import io.gravitee.management.model.ApplicationEntity;
import io.gravitee.management.model.UpdateApplicationEntity;
import io.gravitee.management.model.notification.NotifierEntity;
import io.gravitee.management.model.permissions.RolePermission;
import io.gravitee.management.model.permissions.RolePermissionAction;
import io.gravitee.management.rest.security.Permission;
import io.gravitee.management.rest.security.Permissions;
import io.gravitee.management.service.ApplicationService;
import io.gravitee.management.service.NotifierService;
import io.gravitee.repository.management.model.NotificationReferenceType;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = {"Application"})
public class ApplicationResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private ApplicationService applicationService;

    @Inject
    private NotifierService notifierService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get an application",
            notes = "User must have the READ permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Application", response = ApplicationEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.APPLICATION_DEFINITION, acls = RolePermissionAction.READ)
    })
    public ApplicationEntity getApplication(@PathParam("application") String application) {
        return applicationService.findById(application);
    }
    
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Update an application",
            notes = "User must have the MANAGE_APPLICATION permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Updated application", response = ApplicationEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.APPLICATION_DEFINITION, acls = RolePermissionAction.UPDATE)
    })
    public ApplicationEntity updateApplication(
            @PathParam("application") String application,
            @Valid @NotNull final UpdateApplicationEntity updatedApplication) {
        return applicationService.update(application, updatedApplication);
    }

    @DELETE
    @ApiOperation(value = "Delete an application",
            notes = "User must have the DELETE permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 204, message = "Application successfully deleted"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.APPLICATION_DEFINITION, acls = RolePermissionAction.DELETE)
    })
    public Response deleteApplication(@PathParam("application") String application) {
        applicationService.archive(application);
        return Response.noContent().build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("notifiers")
    @Permissions({
            @Permission(value = RolePermission.APPLICATION_NOTIFICATION, acls = RolePermissionAction.READ)
    })
    public List<NotifierEntity> getNotifiers(@PathParam("application") String application) {
        return notifierService.list(NotificationReferenceType.APPLICATION, application);
    }

    @Path("members")
    public ApplicationMembersResource getApplicationMembersResource() {
        return resourceContext.getResource(ApplicationMembersResource.class);
    }

    @Path("subscriptions")
    public ApplicationSubscriptionsResource getApplicationSubscriptionsResource() {
        return resourceContext.getResource(ApplicationSubscriptionsResource.class);
    }

    @Path("subscribed")
    public ApplicationSubscribedResource getApplicationSubscribedResource() {
        return resourceContext.getResource(ApplicationSubscribedResource.class);
    }

    @Path("analytics")
    public ApplicationAnalyticsResource getApplicationAnalyticsResource() {
        return resourceContext.getResource(ApplicationAnalyticsResource.class);
    }

    @Path("logs")
    public ApplicationLogsResource getApplicationLogsResource() {
        return resourceContext.getResource(ApplicationLogsResource.class);
    }

    @Path("notificationsettings")
    public ApplicationNotificationSettingsResource getNotificationSettingsResource() {
        return resourceContext.getResource(ApplicationNotificationSettingsResource.class);
    }

    @Path("alerts")
    public ApplicationAlertsResource getApplicationAlertsResource() {
        return resourceContext.getResource(ApplicationAlertsResource.class);
    }
}
