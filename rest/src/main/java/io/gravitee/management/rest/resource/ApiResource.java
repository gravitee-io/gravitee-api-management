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
import io.gravitee.management.model.UpdateApiEntity;
import io.gravitee.management.service.ApiService;
import io.gravitee.management.service.exceptions.ApiNotFoundException;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Optional;

/**
 * Defines the REST resources to manage API.
 *
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class ApiResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private ApiService apiService;

    @PathParam("apiName")
    private String apiName;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public ApiEntity get() throws ApiNotFoundException {
        Optional<ApiEntity> optApi = apiService.findByName(apiName);

        if (! optApi.isPresent()) {
            throw new ApiNotFoundException(apiName);
        }

        return optApi.get();
    }

    @POST
    public Response doLifecycleAction(@QueryParam("action") LifecycleActionParam action) {
        Optional<ApiEntity> optApi = apiService.findByName(apiName);

        if (! optApi.isPresent()) {
            throw new ApiNotFoundException(apiName);
        }

        ApiEntity api = optApi.get();
        switch (action.getAction()) {
            case START:
                apiService.start(api.getName());
                break;
            case STOP:
                apiService.stop(api.getName());
                break;
            default:
                break;
        }

        return Response.noContent().build();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ApiEntity update(@Valid final UpdateApiEntity api) {
        return apiService.update(apiName, api);
    }

    @DELETE
    public Response delete() {
        Optional<ApiEntity> optApi = apiService.findByName(apiName);

        if (! optApi.isPresent()) {
            throw new ApiNotFoundException(apiName);
        }

        apiService.delete(apiName);
        return Response.noContent().build();
    }

    @Path("policies")
    public PoliciesConfigurationResource getPoliciesConfigurationResource() {
        return resourceContext.getResource(PoliciesConfigurationResource.class);
    }
}
