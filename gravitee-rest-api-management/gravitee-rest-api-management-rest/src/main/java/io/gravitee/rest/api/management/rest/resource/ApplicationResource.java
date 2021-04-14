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
import io.gravitee.repository.management.model.ApplicationType;
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
import io.gravitee.rest.api.service.configuration.application.ApplicationTypeService;
import io.gravitee.rest.api.service.exceptions.ApplicationNotFoundException;
import io.swagger.annotations.*;
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
@Api(tags = { "Applications" })
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
    @ApiParam(name = "application", required = true)
    private String application;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get an application", notes = "User must have the READ permission to use this service")
    @ApiResponses(
        {
            @ApiResponse(code = 200, message = "Application", response = ApplicationEntity.class),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    @Permissions({ @Permission(value = RolePermission.APPLICATION_DEFINITION, acls = RolePermissionAction.READ) })
    public ApplicationEntity getApplication() {
        return applicationService.findById(application);
    }

    @GET
    @Path("configuration")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
        value = "Get application type definition of an application",
        notes = "User must have the READ permission to use this service"
    )
    @ApiResponses(
        {
            @ApiResponse(code = 200, message = "ApplicationType", response = ApplicationType.class),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    @Permissions({ @Permission(value = RolePermission.APPLICATION_DEFINITION, acls = RolePermissionAction.READ) })
    public Response getApplicationType() {
        ApplicationEntity applicationEntity = applicationService.findById(application);
        ApplicationTypeEntity applicationType = applicationTypeService.getApplicationType(applicationEntity.getType());
        return Response.ok(applicationType).build();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
        value = "Update an application",
        notes = "User must have APPLICATION_DEFINITION[UPDATE] permission to update an application."
    )
    @ApiResponses(
        {
            @ApiResponse(code = 200, message = "Updated application", response = ApplicationEntity.class),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
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

        return applicationService.update(application, updatedApplication);
    }

    @GET
    @Path("picture")
    @ApiOperation(value = "Get the application's picture", notes = "User must have the READ permission to use this service")
    @ApiResponses(
        { @ApiResponse(code = 200, message = "Application's picture"), @ApiResponse(code = 500, message = "Internal server error") }
    )
    @Permissions({ @Permission(value = RolePermission.APPLICATION_DEFINITION, acls = RolePermissionAction.READ) })
    public Response getApplicationPicture(@Context Request request) throws ApplicationNotFoundException {
        PictureEntity picture = applicationService.getPicture(application);

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
    @ApiOperation(
        value = "Renew the client secret for an OAuth2 application",
        notes = "User must have APPLICATION_DEFINITION[UPDATE] permission to update an application."
    )
    @ApiResponses(
        {
            @ApiResponse(code = 200, message = "Updated application", response = ApplicationEntity.class),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    @Permissions({ @Permission(value = RolePermission.APPLICATION_DEFINITION, acls = RolePermissionAction.UPDATE) })
    public ApplicationEntity renewApplicationClientSecret() {
        return applicationService.renewClientSecret(application);
    }

    @DELETE
    @ApiOperation(value = "Delete an application", notes = "User must have the DELETE permission to use this service")
    @ApiResponses(
        {
            @ApiResponse(code = 204, message = "Application successfully deleted"),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    @Permissions({ @Permission(value = RolePermission.APPLICATION_DEFINITION, acls = RolePermissionAction.DELETE) })
    public Response deleteApplication() {
        applicationService.archive(application);
        return Response.noContent().build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("notifiers")
    @ApiOperation(
        value = "List available notifiers for application",
        notes = "User must have the APPLICATION_NOTIFICATION[READ] permission to use this service"
    )
    @ApiResponses(
        {
            @ApiResponse(code = 200, message = "LList of notifiers", response = NotifierEntity.class, responseContainer = "List"),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
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
}
