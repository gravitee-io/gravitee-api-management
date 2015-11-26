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
import io.gravitee.management.rest.enhancer.ApplicationEnhancer;
import io.gravitee.management.service.ApplicationService;
import io.gravitee.management.service.PermissionService;
import io.gravitee.management.service.PermissionType;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
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

    @Inject
    private ApplicationEnhancer applicationEnhancer;

    @PathParam("application")
    private String application;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public ApplicationEntity get() {
        ApplicationEntity applicationEntity = applicationService.findById(application);

        permissionService.hasPermission(getAuthenticatedUser(), application, PermissionType.VIEW_APPLICATION);

        return applicationEnhancer.enhance(getAuthenticatedUser()).apply(applicationEntity);
    }
    
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ApplicationEntity update(final UpdateApplicationEntity updatedApplication) {
        applicationService.findById(this.application);

        permissionService.hasPermission(getAuthenticatedUser(), application, PermissionType.EDIT_APPLICATION);

        return applicationEnhancer.enhance(getAuthenticatedUser()).apply(
                applicationService.update(application, updatedApplication)
        );
    }

    @DELETE
    public Response delete() {
        ApplicationEntity applicationEntity = applicationService.findById(application);
        applicationService.delete(applicationEntity.getName());

        return Response.noContent().build();
    }

    @Path("members")
    public ApplicationMembersResource getApplicationMembersResource() {
        return resourceContext.getResource(ApplicationMembersResource.class);
    }

    @Path("keys")
    public ApiKeyResource getApiKeyResource() {
        return resourceContext.getResource(ApiKeyResource.class);
    }
}
