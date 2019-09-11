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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

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
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApplicationResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Context
    private UriInfo uriInfo;
    
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
    public Response updateApplicationByApplicationId(@PathParam("applicationId") String applicationId, @Valid Application application) {
        
        if(!application.getId().equalsIgnoreCase(applicationId)) {
            return Response
                     .status(Response.Status.BAD_REQUEST)
                     .entity("'applicationId' is not the same that the application in payload")
                     .build();
        }
        
        ApplicationEntity appEntity = applicationService.findById(applicationId);

        if(!getAuthenticatedUser().equals(appEntity.getPrimaryOwner().getId())) {
            throw new ForbiddenAccessException();
        }
        
        UpdateApplicationEntity updateApplicationEntity = new UpdateApplicationEntity();
        updateApplicationEntity.setDescription(application.getDescription());
        updateApplicationEntity.setGroups(new HashSet<String>(application.getGroups()));
        updateApplicationEntity.setName(application.getName());
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
                oacs.setRenewClientSecretSupported(application.getSettings().getOauth().getRenewClientSecretSupported().booleanValue());
                oacs.setResponseTypes(application.getSettings().getOauth().getResponseTypes());
                settings.setoAuthClient(oacs);
            }
            updateApplicationEntity.setSettings(settings);
        }
        
        Application updatedApp = applicationMapper.convert(applicationService.update(applicationId, updateApplicationEntity));
        return Response
                .ok(addApplicationLinks(updatedApp))
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

        CacheControl cc = new CacheControl();
        cc.setNoTransform(true);
        cc.setMustRevalidate(false);
        cc.setNoCache(false);
        cc.setMaxAge(86400);

        InlinePictureEntity image = applicationService.getPicture(applicationId);

        EntityTag etag = new EntityTag(Integer.toString(new String(image.getContent()).hashCode()));
        Response.ResponseBuilder builder = request.evaluatePreconditions(etag);

        if (builder != null) {
            // Preconditions are not met, returning HTTP 304 'not-modified'
            return builder
                    .cacheControl(cc)
                    .build();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(image.getContent(), 0, image.getContent().length);

        return Response
                .ok(baos)
                .cacheControl(cc)
                .tag(etag)
                .type(image.getType())
                .build();
    }
    
    @PUT
    @Path("/picture")
    @Consumes(MediaType.WILDCARD)
    @Produces({ MediaType.WILDCARD, MediaType.APPLICATION_JSON })
    public Response updateApplicationPictureByApplicationId(@Context HttpHeaders headers, @PathParam("applicationId") String applicationId, File newPictureFile) throws IOException {
        String newPicture = new String(Files.readAllBytes(newPictureFile.toPath()));
        checkAndScaleImage(newPicture);
        
        ApplicationEntity applicationEntity = applicationService.findById(applicationId);
        UpdateApplicationEntity updateApplicationEntity = new UpdateApplicationEntity();
        
        updateApplicationEntity.setSettings(applicationEntity.getSettings());
        updateApplicationEntity.setName(applicationEntity.getName());
        updateApplicationEntity.setPicture(newPicture);
        updateApplicationEntity.setDescription(applicationEntity.getDescription());
        updateApplicationEntity.setGroups(applicationEntity.getGroups());
        
        ApplicationEntity updatedApp = applicationService.update(applicationId, updateApplicationEntity);
        return Response
                .ok(applicationMapper.convert(updatedApp))
                .tag(Long.toString(updatedApp.getUpdatedAt().getTime()))
                .lastModified(updatedApp.getUpdatedAt())
                .build();
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

        String basePath = uriInfo.getAbsolutePathBuilder().build().toString();
        basePath = basePath.replaceFirst("/_renew_secret", "");
        
        return Response
                .ok(addApplicationLinks(renwedApplication, basePath))
                .build()
                ;
    }
    
    private Application addApplicationLinks(Application application) {
        String basePath = uriInfo.getAbsolutePathBuilder().build().toString();
        return addApplicationLinks(application, basePath);
    }
    
    private Application addApplicationLinks(Application application, String basePath) {
        return application.links(applicationMapper.computeApplicationLinks(basePath));
    }
    
    @Path("members")
    public ApplicationMembersResource getApplicationMembersResource() {
        return resourceContext.getResource(ApplicationMembersResource.class);
    }
 
    @Path("notifications")
    public ApplicationNotificationSettingsResource getApplicationNotificationSettingsResource() {
        return resourceContext.getResource(ApplicationNotificationSettingsResource.class);
    }

    @Path("logs")
    public ApplicationLogsResource getApplicationLogsResource() {
        return resourceContext.getResource(ApplicationLogsResource.class);
    }

    @Path("analytics")
    public ApplicationAnalyticsResource getApplicationAnalyticsResource() {
        return resourceContext.getResource(ApplicationAnalyticsResource.class);
    }    

}
