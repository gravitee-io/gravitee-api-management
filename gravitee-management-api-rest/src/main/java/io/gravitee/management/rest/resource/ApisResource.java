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
import io.gravitee.management.model.ApiListItem;
import io.gravitee.management.model.NewApiEntity;
import io.gravitee.management.model.Visibility;
import io.gravitee.management.service.ApiService;
import io.gravitee.management.service.exceptions.ApiAlreadyExistsException;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Set;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
@Path("/apis")
public class ApisResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private ApiService apiService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Set<ApiListItem> list() {
        if (isAuthenticated()) {
            return apiService.findByUser(getAuthenticatedUser());
        } else {
            return apiService.findByVisibility(Visibility.PUBLIC);
        }
    }

    /**
     * Create a new API for the authenticated user.
     * @param newApiEntity
     * @return
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(@Valid final NewApiEntity newApiEntity) throws ApiAlreadyExistsException {
        ApiEntity newApi = apiService.create(newApiEntity, getAuthenticatedUser());
        if (newApi != null) {
            return Response
                    .created(URI.create("/apis/" + newApi.getId()))
                    .entity(newApi)
                    .build();
        }

        return Response.serverError().build();
    }

    @Path("{api}")
    public ApiResource getApiResource() {
        return resourceContext.getResource(ApiResource.class);
    }
}
