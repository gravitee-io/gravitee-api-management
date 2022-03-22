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
import io.gravitee.repository.management.model.NotificationReferenceType;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.application.ApplicationSettings;
import io.gravitee.rest.api.model.application.SimpleApplicationSettings;
import io.gravitee.rest.api.model.configuration.application.ApplicationTypeEntity;
import io.gravitee.rest.api.model.notification.NotifierEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.NotifierService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.configuration.application.ApplicationTypeService;
import io.gravitee.rest.api.service.exceptions.ApplicationNotFoundException;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.List;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.*;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "Applications")
public class ApplicationResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private ApplicationService applicationService;

    @Inject
    private NotifierService notifierService;

    @Inject
    private ApplicationTypeService applicationTypeService;

    @PathParam("application")
    @Parameter(name = "application", required = true)
    private String application;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get an application", description = "User must have the READ permission to use this service")
    @ApiResponse(
        responseCode = "200",
        description = "Application",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApplicationEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.APPLICATION_DEFINITION, acls = RolePermissionAction.READ) })
    public ApplicationEntity getApplication() {
        return applicationService.findById(GraviteeContext.getCurrentEnvironment(), application);
    }

    @GET
    @Path("configuration")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Get application type definition of an application",
        description = "User must have the READ permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "ApplicationType",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApplicationTypeEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.APPLICATION_DEFINITION, acls = RolePermissionAction.READ) })
    public Response getApplicationType() {
        ApplicationEntity applicationEntity = applicationService.findById(GraviteeContext.getCurrentEnvironment(), application);
        ApplicationTypeEntity applicationType = applicationTypeService.getApplicationType(applicationEntity.getType());
        return Response.ok(applicationType).build();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Update an application",
        description = "User must have APPLICATION_DEFINITION[UPDATE] permission to update an application."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Updated application",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApplicationEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.APPLICATION_DEFINITION, acls = RolePermissionAction.UPDATE) })
    public ApplicationEntity updateApplication(
        @Valid @NotNull(message = "An application must be provided") final UpdateApplicationEntity updatedApplication
    ) {
        // To preserve backward compatibility, ensure that we have at least default settings for simple application type
        if (
            updatedApplication.getSettings() == null ||
            (updatedApplication.getSettings().getoAuthClient() == null && updatedApplication.getSettings().getApp() == null)
        ) {
            ApplicationSettings settings = new ApplicationSettings();
            SimpleApplicationSettings simpleAppSettings = new SimpleApplicationSettings();
            simpleAppSettings.setType(updatedApplication.getType());
            simpleAppSettings.setClientId(updatedApplication.getClientId());
            updatedApplication.setSettings(settings);
        }

        return applicationService.update(
            GraviteeContext.getCurrentOrganization(),
            GraviteeContext.getCurrentEnvironment(),
            application,
            updatedApplication
        );
    }

    @GET
    @Path("picture")
    @Operation(summary = "Get the application's picture", description = "User must have the READ permission to use this service")
    @ApiResponse(
        responseCode = "200",
        description = "Application's picture",
        content = @Content(mediaType = "*/*", schema = @Schema(type = "string", format = "binary"))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.APPLICATION_DEFINITION, acls = RolePermissionAction.READ) })
    public Response getApplicationPicture(@Context Request request) throws ApplicationNotFoundException {
        return getImageResponse(request, applicationService.getPicture(GraviteeContext.getCurrentEnvironment(), application));
    }

    @GET
    @Path("background")
    @Operation(summary = "Get the application's background", description = "User must have the READ permission to use this service")
    @ApiResponse(
        responseCode = "200",
        description = "Application's background",
        content = @Content(mediaType = "*/*", schema = @Schema(type = "string", format = "binary"))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.APPLICATION_DEFINITION, acls = RolePermissionAction.READ) })
    public Response getApplicationBackground(@Context Request request) throws ApplicationNotFoundException {
        return getImageResponse(request, applicationService.getBackground(GraviteeContext.getCurrentEnvironment(), application));
    }

    private Response getImageResponse(final Request request, PictureEntity picture) {
        if (picture instanceof UrlPictureEntity) {
            return Response.temporaryRedirect(URI.create(((UrlPictureEntity) picture).getUrl())).build();
        }

        InlinePictureEntity image = (InlinePictureEntity) picture;
        if (image == null || image.getContent() == null) {
            return Response.ok().build();
        }

        CacheControl cc = new CacheControl();
        cc.setNoTransform(true);
        cc.setMustRevalidate(false);
        cc.setNoCache(false);
        cc.setMaxAge(86400);

        EntityTag etag = new EntityTag(Integer.toString(new String(image.getContent()).hashCode()));
        Response.ResponseBuilder builder = request.evaluatePreconditions(etag);

        if (builder != null) {
            // Preconditions are not met, returning HTTP 304 'not-modified'
            return builder.cacheControl(cc).build();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(image.getContent(), 0, image.getContent().length);

        return Response.ok(baos).cacheControl(cc).tag(etag).type(image.getType()).build();
    }

    @POST
    @Path("/renew_secret")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Renew the client secret for an OAuth2 application",
        description = "User must have APPLICATION_DEFINITION[UPDATE] permission to update an application."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Updated application",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApplicationEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.APPLICATION_DEFINITION, acls = RolePermissionAction.UPDATE) })
    public ApplicationEntity renewApplicationClientSecret() {
        return applicationService.renewClientSecret(
            GraviteeContext.getCurrentOrganization(),
            GraviteeContext.getCurrentEnvironment(),
            application
        );
    }

    @POST
    @Path("/_restore")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Restore the application",
        description = "User must have APPLICATION_DEFINITION[UPDATE] permission to restore an application."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Restored application",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApplicationEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.APPLICATION_DEFINITION, acls = RolePermissionAction.UPDATE) })
    public ApplicationEntity restoreApplication() {
        if (!isAdmin()) {
            throw new ForbiddenAccessException();
        }
        return applicationService.restore(application);
    }

    @DELETE
    @Operation(summary = "Delete an application", description = "User must have the DELETE permission to use this service")
    @ApiResponse(responseCode = "204", description = "Application successfully deleted")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.APPLICATION_DEFINITION, acls = RolePermissionAction.DELETE) })
    public Response deleteApplication() {
        applicationService.archive(application);
        return Response.noContent().build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("notifiers")
    @Operation(
        summary = "List available notifiers for application",
        description = "User must have the APPLICATION_NOTIFICATION[READ] permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "List of notifiers",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            array = @ArraySchema(schema = @Schema(implementation = NotifierEntity.class))
        )
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.APPLICATION_NOTIFICATION, acls = RolePermissionAction.READ) })
    public List<NotifierEntity> getApplicationNotifiers() {
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

    @Path("metadata")
    public ApplicationMetadataResource getApplicationMetadataResource() {
        return resourceContext.getResource(ApplicationMetadataResource.class);
    }

    @Path("apikeys")
    public ApplicationApiKeysResource getApplicationApiKeysResource() {
        return resourceContext.getResource(ApplicationApiKeysResource.class);
    }
}
