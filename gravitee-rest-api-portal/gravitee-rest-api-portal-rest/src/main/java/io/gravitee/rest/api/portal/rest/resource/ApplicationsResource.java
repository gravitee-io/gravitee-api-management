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

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.NewApplicationEntity;
import io.gravitee.rest.api.model.application.ApplicationSettings;
import io.gravitee.rest.api.model.application.OAuthClientSettings;
import io.gravitee.rest.api.model.application.SimpleApplicationSettings;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.portal.rest.mapper.ApplicationMapper;
import io.gravitee.rest.api.portal.rest.model.Application;
import io.gravitee.rest.api.portal.rest.model.ApplicationInput;
import io.gravitee.rest.api.portal.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.portal.rest.security.Permission;
import io.gravitee.rest.api.portal.rest.security.Permissions;
import io.gravitee.rest.api.service.ApplicationService;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApplicationsResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private ApplicationService applicationService;
    
    @Inject
    private ApplicationMapper applicationMapper;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({
        @Permission(value = RolePermission.MANAGEMENT_APPLICATION, acls = RolePermissionAction.CREATE),
    })
    public Response createApplication(@Valid ApplicationInput applicationInput) {
        if(applicationInput == null) {
            throw new BadRequestException("input must not be null");
        }
        NewApplicationEntity newApplicationEntity = new NewApplicationEntity();
        newApplicationEntity.setDescription(applicationInput.getDescription());
        newApplicationEntity.setGroups(new HashSet<String>(applicationInput.getGroups()));
        newApplicationEntity.setName(applicationInput.getName());

        final io.gravitee.rest.api.portal.rest.model.ApplicationSettings settings = applicationInput.getSettings();
        ApplicationSettings newApplicationEntitySettings = new ApplicationSettings();
        
        if(settings == null || (settings.getApp()==null && settings.getOauth() == null)) {
            newApplicationEntity.setSettings(newApplicationEntitySettings);
        } else {
            final io.gravitee.rest.api.portal.rest.model.SimpleApplicationSettings simpleAppInput = settings.getApp();
            if(simpleAppInput != null) {
                SimpleApplicationSettings sas = new SimpleApplicationSettings();
                sas.setClientId(simpleAppInput.getClientId());
                sas.setType(simpleAppInput.getType());
                
                newApplicationEntitySettings.setApp(sas);
            }
            
            final io.gravitee.rest.api.portal.rest.model.OAuthClientSettings oauthAppInput = settings.getOauth();
            if (oauthAppInput != null) {
                OAuthClientSettings ocs = new OAuthClientSettings();
                ocs.setApplicationType(oauthAppInput.getApplicationType());
                ocs.setClientId(oauthAppInput.getClientId());
                ocs.setClientSecret(oauthAppInput.getClientSecret());
                ocs.setClientUri(oauthAppInput.getClientUri());
                ocs.setClientId(oauthAppInput.getClientId());
                ocs.setLogoUri(oauthAppInput.getLogoUri());
                ocs.setGrantTypes(oauthAppInput.getGrantTypes());
                ocs.setRedirectUris(oauthAppInput.getRedirectUris());
                ocs.setRenewClientSecretSupported(oauthAppInput.getRenewClientSecretSupported().booleanValue());
                ocs.setResponseTypes(oauthAppInput.getResponseTypes());
    
                newApplicationEntitySettings.setoAuthClient(ocs);
            }
        }
        newApplicationEntity.setSettings(newApplicationEntitySettings);
        
        ApplicationEntity createdApplicationEntity = applicationService.create(newApplicationEntity, getAuthenticatedUser());
        
        return Response
                .status(Response.Status.CREATED)
                .entity(applicationMapper.convert(createdApplicationEntity))
                .build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({
        @Permission(value = RolePermission.MANAGEMENT_APPLICATION, acls = RolePermissionAction.READ),
        @Permission(value = RolePermission.PORTAL_APPLICATION, acls = RolePermissionAction.READ)
    })
    public Response getApplications(@BeanParam PaginationParam paginationParam) {
        
        List<Application> applicationsList = applicationService.findByUser(getAuthenticatedUser())
                .stream()
                .map(applicationMapper::convert)
                .map(this::addApplicationLinks)
                .collect(Collectors.toList())
                ;
        
        return createListResponse(applicationsList, paginationParam);
    }
    
    private Application addApplicationLinks(Application application) {
        String basePath = uriInfo.getAbsolutePathBuilder().path(application.getId()).build().toString();
        return application.links(applicationMapper.computeApplicationLinks(basePath));
    }
    
    @Path("{applicationId}")
    public ApplicationResource getApplicationResource() {
        return resourceContext.getResource(ApplicationResource.class);
    }
    
}
