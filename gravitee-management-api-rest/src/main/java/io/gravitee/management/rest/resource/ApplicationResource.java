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

import io.gravitee.common.http.MediaType;
import io.gravitee.management.model.ApplicationEntity;
import io.gravitee.management.model.UpdateApplicationEntity;
import io.gravitee.management.model.permissions.ApplicationPermission;
import io.gravitee.management.rest.enhancer.ApplicationEnhancer;
import io.gravitee.management.rest.security.ApplicationPermissionsRequired;
import io.gravitee.management.service.ApplicationService;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
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
    private ApplicationEnhancer applicationEnhancer;

    @PathParam("application")
    private String application;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApplicationPermissionsRequired(ApplicationPermission.READ)
    public ApplicationEntity get() {
        ApplicationEntity applicationEntity = applicationService.findById(application);
        return applicationEnhancer.enhance(securityContext).apply(applicationEntity);
    }
    
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApplicationPermissionsRequired(ApplicationPermission.MANAGE_API)
    public ApplicationEntity update(@Valid @NotNull final UpdateApplicationEntity updatedApplication) {
        return applicationEnhancer.enhance(securityContext).apply(
                applicationService.update(application, updatedApplication)
        );
    }

    @DELETE
    @ApplicationPermissionsRequired(ApplicationPermission.DELETE)
    public Response delete() {
        applicationService.delete(application);

        return Response.noContent().build();
    }

    @Path("members")
    public ApplicationMembersResource getApplicationMembersResource() {
        return resourceContext.getResource(ApplicationMembersResource.class);
    }

    @Path("keys")
    public ApplicationApiKeysResource getApiKeyResource() {
        return resourceContext.getResource(ApplicationApiKeysResource.class);
    }
}
