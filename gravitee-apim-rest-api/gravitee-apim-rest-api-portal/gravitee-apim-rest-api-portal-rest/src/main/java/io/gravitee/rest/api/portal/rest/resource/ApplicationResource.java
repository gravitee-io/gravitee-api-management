/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.portal.rest.resource;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.ApiKeyMode;
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.InlinePictureEntity;
import io.gravitee.rest.api.model.UpdateApplicationEntity;
import io.gravitee.rest.api.model.application.ApplicationSettings;
import io.gravitee.rest.api.model.application.OAuthClientSettings;
import io.gravitee.rest.api.model.application.SimpleApplicationSettings;
import io.gravitee.rest.api.model.application.TlsSettings;
import io.gravitee.rest.api.model.configuration.application.ApplicationTypeEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.portal.rest.mapper.ApplicationMapper;
import io.gravitee.rest.api.portal.rest.model.Application;
import io.gravitee.rest.api.portal.rest.utils.PortalApiLinkHelper;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.configuration.application.ApplicationTypeService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import java.util.Date;

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

    @Inject
    private ApplicationTypeService applicationTypeService;

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.APPLICATION_DEFINITION, acls = RolePermissionAction.DELETE) })
    public Response deleteApplicationByApplicationId(@PathParam("applicationId") String applicationId) {
        applicationService.archive(GraviteeContext.getExecutionContext(), applicationId);
        return Response.noContent().build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.APPLICATION_DEFINITION, acls = RolePermissionAction.READ) })
    public Response getApplicationByApplicationId(@PathParam("applicationId") String applicationId) {
        Application application = applicationMapper.convert(
            GraviteeContext.getExecutionContext(),
            applicationService.findById(GraviteeContext.getExecutionContext(), applicationId),
            uriInfo
        );

        return Response.ok(addApplicationLinks(application)).build();
    }

    @GET
    @Path("configuration")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.APPLICATION_DEFINITION, acls = RolePermissionAction.READ) })
    public Response getApplicationType(@PathParam("applicationId") String applicationId) {
        ApplicationEntity applicationEntity = applicationService.findById(GraviteeContext.getExecutionContext(), applicationId);
        ApplicationTypeEntity applicationType = applicationTypeService.getApplicationType(applicationEntity.getType());
        return Response.ok(applicationType).build();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.APPLICATION_DEFINITION, acls = RolePermissionAction.UPDATE) })
    public Response updateApplicationByApplicationId(
        @PathParam("applicationId") String applicationId,
        @Valid @NotNull(message = "Input must not be null.") Application application
    ) {
        if (!applicationId.equalsIgnoreCase(application.getId())) {
            throw new BadRequestException("'applicationId' is not the same that the application in payload");
        }

        ApplicationEntity appEntity = applicationService.findById(GraviteeContext.getExecutionContext(), applicationId);

        UpdateApplicationEntity updateApplicationEntity = new UpdateApplicationEntity();
        updateApplicationEntity.setDescription(application.getDescription());
        updateApplicationEntity.setDomain(application.getDomain());
        updateApplicationEntity.setName(application.getName());
        updateApplicationEntity.setPicture(checkAndScaleImage(application.getPicture()));
        checkImageFormat(application.getBackground());
        updateApplicationEntity.setBackground(application.getBackground());
        updateApplicationEntity.setGroups(appEntity.getGroups());
        if (application.getApiKeyMode() != null) {
            updateApplicationEntity.setApiKeyMode(ApiKeyMode.valueOf(application.getApiKeyMode().name()));
        }

        if (application.getSettings() != null) {
            ApplicationSettings settings = new ApplicationSettings();

            if (application.getSettings().getApp() != null) {
                SimpleApplicationSettings sas = appEntity.getSettings().getApp();
                sas.setClientId(application.getSettings().getApp().getClientId());
                sas.setType(application.getSettings().getApp().getType());
                settings.setApp(sas);
            } else if (application.getSettings().getOauth() != null) {
                OAuthClientSettings oacs = appEntity.getSettings().getOauth();
                oacs.setGrantTypes(application.getSettings().getOauth().getGrantTypes());
                oacs.setRedirectUris(application.getSettings().getOauth().getRedirectUris());
                settings.setOauth(oacs);
            }
            if (application.getSettings().getTls() != null) {
                settings.setTls(TlsSettings.builder().clientCertificate(application.getSettings().getTls().getClientCertificate()).build());
            }
            updateApplicationEntity.setSettings(settings);
        }

        Application updatedApp = applicationMapper.convert(
            GraviteeContext.getExecutionContext(),
            applicationService.update(GraviteeContext.getExecutionContext(), applicationId, updateApplicationEntity),
            uriInfo
        );
        return Response
            .ok(addApplicationLinks(updatedApp))
            .tag(Long.toString(updatedApp.getUpdatedAt().toInstant().toEpochMilli()))
            .lastModified(Date.from(updatedApp.getUpdatedAt().toInstant()))
            .build();
    }

    @GET
    @Path("picture")
    @Produces({ MediaType.WILDCARD, MediaType.APPLICATION_JSON })
    @Permissions({ @Permission(value = RolePermission.APPLICATION_DEFINITION, acls = RolePermissionAction.READ) })
    public Response getPictureByApplicationId(@Context Request request, @PathParam("applicationId") String applicationId) {
        applicationService.findById(GraviteeContext.getExecutionContext(), applicationId);
        InlinePictureEntity image = applicationService.getPicture(GraviteeContext.getExecutionContext(), applicationId);
        return createPictureResponse(request, image);
    }

    @GET
    @Path("background")
    @Produces({ MediaType.WILDCARD, MediaType.APPLICATION_JSON })
    @Permissions({ @Permission(value = RolePermission.APPLICATION_DEFINITION, acls = RolePermissionAction.READ) })
    public Response getBackgroundByApplicationId(@Context Request request, @PathParam("applicationId") String applicationId) {
        applicationService.findById(GraviteeContext.getExecutionContext(), applicationId);
        InlinePictureEntity image = applicationService.getBackground(GraviteeContext.getExecutionContext(), applicationId);
        return createPictureResponse(request, image);
    }

    @POST
    @Path("/_renew_secret")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.APPLICATION_DEFINITION, acls = RolePermissionAction.UPDATE) })
    public Response renewApplicationSecret(@PathParam("applicationId") String applicationId) {
        Application renwedApplication = applicationMapper.convert(
            GraviteeContext.getExecutionContext(),
            applicationService.renewClientSecret(GraviteeContext.getExecutionContext(), applicationId),
            uriInfo
        );

        return Response.ok(addApplicationLinks(renwedApplication)).build();
    }

    private Application addApplicationLinks(Application application) {
        return application.links(
            applicationMapper.computeApplicationLinks(
                PortalApiLinkHelper.applicationsURL(uriInfo.getBaseUriBuilder(), application.getId()),
                application.getUpdatedAt()
            )
        );
    }

    @Path("members")
    public ApplicationMembersResource getApplicationMembersResource() {
        return resourceContext.getResource(ApplicationMembersResource.class);
    }

    @Path("metadata")
    public ApplicationMetadataResource getApplicationMetadataResource() {
        return resourceContext.getResource(ApplicationMetadataResource.class);
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

    @Path("alerts")
    public ApplicationAlertsResource getApplicationAlertsResource() {
        return resourceContext.getResource(ApplicationAlertsResource.class);
    }

    @Path("keys")
    public ApplicationKeysResource getApplicationKeysResource() {
        return resourceContext.getResource(ApplicationKeysResource.class);
    }
}
