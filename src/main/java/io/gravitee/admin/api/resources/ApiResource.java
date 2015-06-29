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
package io.gravitee.admin.api.resources;

import static javax.ws.rs.core.HttpHeaders.LOCATION;

import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.core.service.ApiService;
import io.gravitee.gateway.core.service.impl.ApiServiceImpl;
import io.gravitee.model.Api;

/**
 * Defines the REST resources to manage {@code Api}.
 *
 * @author Azize Elamrani (azize dot elamrani at gmail dot com)
 */
@Singleton
@Produces(MediaType.APPLICATION_JSON)
@Path("/apis")
public class ApiResource {

    private ApiService apiService = new ApiServiceImpl();

    @GET
    public Response listAll() {
        return Response.status(HttpStatusCode.OK_200).entity(apiService.listAll())
                .header("Access-Control-Allow-Origin", "*").build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(final Api api) {
        if (apiService.create(api)) {
            return Response.status(HttpStatusCode.CREATED_201).header(LOCATION, "/rest/apis/" +
                    api.getName()).entity(api)
                    .header("Access-Control-Allow-Origin", "*").build();
        } else {
            return Response.status(HttpStatusCode.BAD_REQUEST_400).build();
        }
    }

    @POST
    @Path("/start/{name}")
    public Response startApi(@PathParam("name") final String name) {
        apiService.start(name);
        return Response.status(HttpStatusCode.OK_200)
                .header("Access-Control-Allow-Origin", "*").build();
    }

    @POST
    @Path("/stop/{name}")
    public Response stopApi(@PathParam("name") final String name) {
        apiService.stop(name);
        return Response.status(HttpStatusCode.OK_200)
                .header("Access-Control-Allow-Origin", "*").build();
    }

    @POST
    @Path("/reload/{name}")
    public Response reloadApi(@PathParam("name") final String name) {
        if (apiService.reload(name)) {
            return Response.status(HttpStatusCode.OK_200)
                    .header("Access-Control-Allow-Origin", "*").build();
        } else {
            return Response.status(HttpStatusCode.BAD_REQUEST_400).build();
        }
    }
}
