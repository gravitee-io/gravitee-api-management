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
package io.gravitee.rest.api.portal.rest.resource;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.InlinePictureEntity;
import io.gravitee.rest.api.model.UpdateApplicationEntity;
import io.gravitee.rest.api.model.application.ApplicationSettings;
import io.gravitee.rest.api.model.application.OAuthClientSettings;
import io.gravitee.rest.api.model.application.SimpleApplicationSettings;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.portal.rest.mapper.ApplicationMapper;
import io.gravitee.rest.api.portal.rest.model.Application;
import io.gravitee.rest.api.portal.rest.security.Permission;
import io.gravitee.rest.api.portal.rest.security.Permissions;
import io.gravitee.rest.api.portal.rest.utils.PortalApiLinkHelper;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import java.util.Date;
import java.util.HashSet;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApplicationResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private ApplicationService applicationService;

    @Inject
    private ApplicationMapper applicationMapper;

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({
        @Permission(value = RolePermission.APPLICATION_DEFINITION, acls = RolePermissionAction.DELETE)
    })
    public Response deleteApplicationByApplicationId(@PathParam("applicationId") String applicationId) {
        applicationService.archive(applicationId);
        return Response
                .noContent()
                .build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({
        @Permission(value = RolePermission.APPLICATION_DEFINITION, acls = RolePermissionAction.READ)
    })
    public Response getApplicationByApplicationId(@PathParam("applicationId") String applicationId) {
        Application application = applicationMapper.convert(applicationService.findById(applicationId));

        return Response
                .ok(addApplicationLinks(application))
                .build()
                ;
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({
        @Permission(value = RolePermission.APPLICATION_DEFINITION, acls = RolePermissionAction.UPDATE)
    })
    public Response updateApplicationByApplicationId(@PathParam("applicationId") String applicationId, @Valid @NotNull(message="Input must not be null.") Application application) {

        if (!applicationId.equalsIgnoreCase(application.getId())) {
            throw new BadRequestException("'applicationId' is not the same that the application in payload");
        }

        ApplicationEntity appEntity = applicationService.findById(applicationId);

        if(!getAuthenticatedUser().equals(appEntity.getPrimaryOwner().getId())) {
            throw new ForbiddenAccessException();
        }

        UpdateApplicationEntity updateApplicationEntity = new UpdateApplicationEntity();
        updateApplicationEntity.setDescription(application.getDescription());
        if (application.getGroups() != null) {
            updateApplicationEntity.setGroups(new HashSet<>(application.getGroups()));
        }
        updateApplicationEntity.setName(application.getName());
        updateApplicationEntity.setPicture(checkAndScaleImage(application.getPicture()));

        if(application.getSettings() != null) {
            ApplicationSettings settings = new ApplicationSettings();
            if(application.getSettings().getApp() != null) {
                SimpleApplicationSettings sas = new SimpleApplicationSettings();
                sas.setClientId(application.getSettings().getApp().getClientId());
                sas.setType(application.getSettings().getApp().getType());
                settings.setApp(sas);
            } else if(application.getSettings().getOauth() != null) {
                OAuthClientSettings oacs = new OAuthClientSettings();
                oacs.setApplicationType(application.getSettings().getOauth().getApplicationType());
                oacs.setClientId(application.getSettings().getOauth().getClientId());
                oacs.setClientSecret(application.getSettings().getOauth().getClientSecret());
                oacs.setClientUri(application.getSettings().getOauth().getClientUri());
                oacs.setGrantTypes(application.getSettings().getOauth().getGrantTypes());
                oacs.setLogoUri(application.getSettings().getOauth().getLogoUri());
                oacs.setRedirectUris(application.getSettings().getOauth().getRedirectUris());
                oacs.setRenewClientSecretSupported(application.getSettings().getOauth().getRenewClientSecretSupported());
                oacs.setResponseTypes(application.getSettings().getOauth().getResponseTypes());
                settings.setoAuthClient(oacs);
            }
            updateApplicationEntity.setSettings(settings);
        }

        Application updatedApp = applicationMapper.convert(applicationService.update(applicationId, updateApplicationEntity));
        return Response
                .ok(addApplicationLinks(updatedApp))
                .tag(Long.toString(updatedApp.getUpdatedAt().toInstant().toEpochMilli()))
                .lastModified(Date.from(updatedApp.getUpdatedAt().toInstant()))
                .build();
    }

    @GET
    @Path("picture")
    @Produces({MediaType.WILDCARD, MediaType.APPLICATION_JSON})
    @Permissions({
        @Permission(value = RolePermission.APPLICATION_DEFINITION, acls = RolePermissionAction.READ)
    })
    public Response getPictureByApplicationId(@Context Request request, @PathParam("applicationId") String applicationId) {
        applicationService.findById(applicationId);

        InlinePictureEntity image = applicationService.getPicture(applicationId);

        return createPictureResponse(request, image);
    }

    @POST
    @Path("/_renew_secret")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({
        @Permission(value = RolePermission.APPLICATION_DEFINITION, acls = RolePermissionAction.UPDATE)
    })
    public Response renewApplicationSecret(@PathParam("applicationId") String applicationId) {

        Application renwedApplication = applicationMapper.convert(applicationService.renewClientSecret(applicationId));

        return Response
                .ok(addApplicationLinks(renwedApplication))
                .build()
                ;
    }

    private Application addApplicationLinks(Application application) {
        return application.links(applicationMapper.computeApplicationLinks(
                PortalApiLinkHelper.applicationsURL(uriInfo.getBaseUriBuilder(), application.getId()), application.getUpdatedAt()));
    }


    @Path("members")
    public ApplicationMembersResource getApplicationMembersResource() {
        return resourceContext.getResource(ApplicationMembersResource.class);
    }

    @Path("notifications")
    public ApplicationNotificationResource getApplicationNotificationResource() {
        return resourceContext.getResource(ApplicationNotificationResource.class);
    }

    @Path("logs")
    public ApplicationLogsResource getApplicationLogsResource() {
        return resourceContext.getResource(ApplicationLogsResource.class);
    }

    @Path("analytics")
    public ApplicationAnalyticsResource getApplicationAnalyticsResource() {
        return resourceContext.getResource(ApplicationAnalyticsResource.class);
    }

    @Path("subscribers")
    public ApplicationSubscribersResource getApplicationSubscribersResource() {
        return resourceContext.getResource(ApplicationSubscribersResource.class);
    }
}
