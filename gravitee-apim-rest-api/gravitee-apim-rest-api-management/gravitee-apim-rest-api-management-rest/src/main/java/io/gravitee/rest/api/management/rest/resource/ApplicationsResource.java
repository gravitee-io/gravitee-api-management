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
import io.gravitee.repository.management.model.ApplicationStatus;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.NewApplicationEntity;
import io.gravitee.rest.api.model.application.ApplicationListItem;
import io.gravitee.rest.api.model.application.ApplicationSettings;
import io.gravitee.rest.api.model.application.SimpleApplicationSettings;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;
import io.gravitee.rest.api.service.notification.ApplicationHook;
import io.gravitee.rest.api.service.notification.Hook;
import io.swagger.annotations.*;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = { "Applications" })
public class ApplicationsResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private ApplicationService applicationService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
        value = "List all the applications accessible to authenticated user",
        notes = "User must have MANAGEMENT_APPLICATION[READ] and PORTAL_APPLICATION[READ] permission to list applications."
    )
    @ApiResponses(
        {
            @ApiResponse(code = 200, message = "User's applications", response = ApplicationEntity.class, responseContainer = "List"),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_APPLICATION, acls = RolePermissionAction.READ) })
    public List<ApplicationListItem> getApplications(
        @QueryParam("group") final String group,
        @QueryParam("query") final String query,
        @QueryParam("status") @DefaultValue("ACTIVE") final String status
    ) {
        Set<ApplicationListItem> applications;

        if (!isAdmin() && ApplicationStatus.ARCHIVED.name().equalsIgnoreCase(status)) {
            throw new ForbiddenAccessException();
        }

        if (query != null && !query.trim().isEmpty()) {
            applications =
                applicationService.findByUserAndNameAndStatus(
                    getAuthenticatedUser(),
                    isAdmin(),
                    query,
                    status,
                    GraviteeContext.getCurrentEnvironment(),
                    GraviteeContext.getCurrentOrganization()
                );
        } else if (isAdmin()) {
            applications =
                group != null
                    ? applicationService.findByGroupsAndStatus(
                        GraviteeContext.getCurrentOrganization(),
                        Collections.singletonList(group),
                        status
                    )
                    : applicationService.findByStatus(
                        GraviteeContext.getCurrentOrganization(),
                        GraviteeContext.getCurrentEnvironment(),
                        status
                    );
        } else {
            applications =
                applicationService.findByUser(
                    GraviteeContext.getCurrentOrganization(),
                    GraviteeContext.getCurrentEnvironment(),
                    getAuthenticatedUser()
                );
            if (group != null && !group.isEmpty()) {
                applications =
                    applications
                        .stream()
                        .filter(app -> app.getGroups() != null && app.getGroups().contains(group))
                        .collect(Collectors.toSet());
            }
        }

        applications.forEach(application -> this.addPictureUrl(application));

        return applications
            .stream()
            .sorted(
                (o1, o2) ->
                    ApplicationStatus.ACTIVE.name().equalsIgnoreCase(status)
                        ? String.CASE_INSENSITIVE_ORDER.compare(o1.getName(), o2.getName())
                        : o2.getUpdatedAt().compareTo(o1.getUpdatedAt())
            )
            .collect(Collectors.toList());
    }

    private void addPictureUrl(ApplicationListItem application) {
        final UriBuilder ub = uriInfo.getBaseUriBuilder();
        final UriBuilder uriBuilder = ub
            .path("organizations")
            .path(GraviteeContext.getCurrentOrganization())
            .path("environments")
            .path(GraviteeContext.getCurrentEnvironment())
            .path("applications")
            .path(application.getId())
            .path("picture");
        // force browser to get if updated
        uriBuilder.queryParam("hash", application.getUpdatedAt().getTime());
        application.setPicture(null);
        application.setPictureUrl(uriBuilder.build().toString());
    }

    /**
     * Create a new application for the authenticated user.
     *
     * @param application
     * @return
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(
        value = "Create an application",
        notes = "User must have MANAGEMENT_APPLICATION[CREATE] permission to create an application."
    )
    @ApiResponses(
        {
            @ApiResponse(code = 201, message = "Application successfully created", response = ApplicationEntity.class),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_APPLICATION, acls = RolePermissionAction.CREATE) })
    public Response createApplication(
        @ApiParam(name = "application", required = true) @Valid @NotNull(
            message = "An application must be provided"
        ) final NewApplicationEntity application
    ) {
        // To preserve backward compatibility, ensure that we have at least default settings for simple application type
        if (
            application.getSettings() == null ||
            (application.getSettings().getoAuthClient() == null && application.getSettings().getApp() == null)
        ) {
            ApplicationSettings settings = new ApplicationSettings();
            SimpleApplicationSettings simpleAppSettings = new SimpleApplicationSettings();
            simpleAppSettings.setType(application.getType());
            simpleAppSettings.setClientId(application.getClientId());
            settings.setApp(simpleAppSettings);
            application.setSettings(settings);
        }

        ApplicationEntity newApplication = applicationService.create(
            GraviteeContext.getCurrentEnvironment(),
            application,
            getAuthenticatedUser()
        );
        if (newApplication != null) {
            return Response.created(this.getLocationHeader(newApplication.getId())).entity(newApplication).build();
        }

        return Response.serverError().build();
    }

    @GET
    @Path("/hooks")
    @ApiOperation("Get the list of available hooks")
    @ApiResponses({ @ApiResponse(code = 200, message = "List of hooks"), @ApiResponse(code = 500, message = "Internal server error") })
    @Produces(MediaType.APPLICATION_JSON)
    public Hook[] getApplicationHooks() {
        return ApplicationHook.values();
    }

    @Path("{application}")
    public ApplicationResource getApplicationResource() {
        return resourceContext.getResource(ApplicationResource.class);
    }
}
