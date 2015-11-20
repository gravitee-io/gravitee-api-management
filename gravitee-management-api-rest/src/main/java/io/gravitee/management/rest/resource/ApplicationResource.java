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

import io.gravitee.management.model.ApplicationEntity;
import io.gravitee.management.model.UpdateApplicationEntity;
import io.gravitee.management.service.ApplicationService;
import io.gravitee.management.service.PermissionService;
import io.gravitee.management.service.PermissionType;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class ApplicationResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private ApplicationService applicationService;

    @Inject
    private PermissionService permissionService;

    @PathParam("applicationName")
    private String applicationName;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public ApplicationEntity get() {
        ApplicationEntity applicationEntity = applicationService.findByName(applicationName);

        permissionService.hasPermission(getAuthenticatedUser(), applicationName, PermissionType.VIEW_APPLICATION);

        return applicationEntity;
    }
    
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ApplicationEntity update(final UpdateApplicationEntity application) {
        ApplicationEntity applicationEntity = applicationService.findByName(applicationName);

        permissionService.hasPermission(getAuthenticatedUser(), applicationEntity.getName(), PermissionType.EDIT_APPLICATION);

        return applicationService.update(applicationEntity.getName(), application);
    }

    @DELETE
    public Response delete() {
        ApplicationEntity applicationEntity = applicationService.findByName(applicationName);
        applicationService.delete(applicationEntity.getName());

        return Response.noContent().build();
    }

    @Path("members")
    public ApplicationMembersResource getApplicationMembersResource() {
        return resourceContext.getResource(ApplicationMembersResource.class);
    }


    @Path("{apiName}")
    public ApiKeyResource getApiKey() {
        return resourceContext.getResource(ApiKeyResource.class);
    }
}
