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
package io.gravitee.management.api.resources;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.repository.api.ApiRepository;
import io.gravitee.repository.model.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Set;

import static javax.ws.rs.core.HttpHeaders.LOCATION;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
@Component
@Produces(MediaType.APPLICATION_JSON)
@Path("/apis")
public class ApisResource {

    @Context
    private ResourceContext resourceContext;

    @Autowired
    private ApiRepository apiRepository;

    @GET
    public Set<Api> listAll() {
        return apiRepository.findAll();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(final Api api) {
        Api createdApi = apiRepository.create(api);
        if (createdApi != null) {
            return Response.status(HttpStatusCode.CREATED_201).header(LOCATION, "/rest/apis/" +
                    api.getName()).entity(createdApi).build();
        } else {
            return Response.status(HttpStatusCode.BAD_REQUEST_400).build();
        }
    }

    @Path("{apiName}")
    public ApiResource getApiResource(@PathParam("apiName") String apiName) {
        ApiResource apiResource = resourceContext.getResource(ApiResource.class);
        apiResource.setApiName(apiName);

        return apiResource;
    }
}
