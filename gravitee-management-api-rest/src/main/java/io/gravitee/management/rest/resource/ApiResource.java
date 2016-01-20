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

import io.gravitee.management.model.ApiEntity;
import io.gravitee.management.model.MemberEntity;
import io.gravitee.management.model.MembershipType;
import io.gravitee.management.model.UpdateApiEntity;
import io.gravitee.management.model.Visibility;
import io.gravitee.management.rest.annotation.Role;
import io.gravitee.management.rest.annotation.RoleType;
import io.gravitee.management.service.ApiService;
import io.gravitee.management.service.PermissionService;
import io.gravitee.management.service.PermissionType;
import io.gravitee.management.service.exceptions.ApiNotFoundException;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * Defines the REST resources to manage API.
 *
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class ApiResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private ApiService apiService;
    
    @Inject
    private PermissionService permissionService;
    
    @PathParam("api")
    private String api;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public ApiEntity get() throws ApiNotFoundException {
        ApiEntity api = apiService.findById(this.api);

        permissionService.hasPermission(getAuthenticatedUser(), this.api, PermissionType.VIEW_API);

        setPermission(api);

        return api;
    }

    @POST
    @Role({RoleType.OWNER, RoleType.TEAM_OWNER})
    public Response doLifecycleAction(@QueryParam("action") LifecycleActionParam action) {
        ApiEntity api = apiService.findById(this.api);

        permissionService.hasPermission(getAuthenticatedUser(), this.api, PermissionType.EDIT_API);

        switch (action.getAction()) {
            case START:
                apiService.start(api.getId());
                break;
            case STOP:
                apiService.stop(api.getId());
                break;
            default:
                break;
        }

        return Response.noContent().build();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Role({RoleType.OWNER, RoleType.TEAM_OWNER})
    public ApiEntity update(@Valid @NotNull final UpdateApiEntity api) {
        permissionService.hasPermission(getAuthenticatedUser(), this.api, PermissionType.EDIT_API);

        final ApiEntity updatedApi = apiService.update(this.api, api);
        setPermission(updatedApi);

        return updatedApi;
    }

    @DELETE
    @Role({RoleType.OWNER, RoleType.TEAM_OWNER})
    public Response delete() {

        permissionService.hasPermission(getAuthenticatedUser(), api, PermissionType.EDIT_API);

        apiService.delete(api);
        
        return Response.noContent().build();
    }
    
    @POST
    @Role({RoleType.OWNER, RoleType.TEAM_OWNER})
    @Produces(MediaType.APPLICATION_JSON)
    @Path("deploy")
    public Response deployAPI() {
        permissionService.hasPermission(getAuthenticatedUser(), this.api, PermissionType.EDIT_API);

        try {
            ApiEntity apiEntity = apiService.deploy(api, getAuthenticatedUsername());
            return Response.status(Status.OK).entity(apiEntity).build();
        } catch (Exception e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("JsonProcessingException " + e).build();
        }
    }

    @GET
    @Role({RoleType.OWNER, RoleType.TEAM_OWNER})
    @Produces(MediaType.APPLICATION_JSON)
    @Path("state")
    public io.gravitee.management.rest.model.ApiEntity isAPISynchronized() {
        permissionService.hasPermission(getAuthenticatedUser(), this.api, PermissionType.EDIT_API);

        io.gravitee.management.rest.model.ApiEntity apiEntity = new io.gravitee.management.rest.model.ApiEntity();
        
        apiEntity.setApiId(this.api);
        setSynchronizationState(apiEntity);
        
        return apiEntity;
    }
       
    @Path("keys")
    public ApiKeysResource getApiKeyResource() {
        return resourceContext.getResource(ApiKeysResource.class);
    }

    @Path("members")
    public ApiMembersResource getApiMembersResource() {
        return resourceContext.getResource(ApiMembersResource.class);
    }

    @Path("analytics")
    public ApiAnalyticsResource getApiAnalyticsResource() {
        return resourceContext.getResource(ApiAnalyticsResource.class);
    }

    @Path("health")
    public ApiHealthResource getApiHealthResource() {
        return resourceContext.getResource(ApiHealthResource.class);
    }

    @Path("pages")
    public ApiPagesResource getApiPagesResource() {
        return resourceContext.getResource(ApiPagesResource.class);
    }
    
    private void setPermission(ApiEntity api) {
        if(isAuthenticated()) {
            MemberEntity member = apiService.getMember(api.getId(), getAuthenticatedUsername());
            if (member != null) {
                api.setPermission(member.getType());
            } else {
                if (api.getVisibility() == Visibility.PUBLIC) {
                    // If API is public, all users have the user permission
                    api.setPermission(MembershipType.USER);
                }
            }
        }
    }
    
    private void setSynchronizationState(io.gravitee.management.rest.model.ApiEntity apiEntity) {
        ApiEntity _apiEntity = apiService.findById(api);
        if (apiService.isAPISynchronized(_apiEntity)) {
            apiEntity.setIsSynchronized(true);
        } else {
            apiEntity.setIsSynchronized(false);
        }
    }
}
