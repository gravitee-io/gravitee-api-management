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

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.model.Api;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.util.Set;

import static javax.ws.rs.core.HttpHeaders.LOCATION;

/**
 * @author Azize Elamrani (azize dot elamrani at gmail dot com)
 */
@Singleton
@Path("/apis")
public class ApiResource extends AbstractResource {

    @GET
    public Set<Api> listAll() {
        return getRegistry().listAll();
    }

    @POST
    public Response create(final Api api) {
        if (getRegistry().createApi(api)) {
            return Response.status(HttpStatusCode.CREATED_201).header(LOCATION, "/rest/apis" +
                    api.getContextPath()).entity(api).build();
        } else {
            return Response.status(HttpStatusCode.BAD_REQUEST_400).build();
        }
    }

    @GET
    @Path("/reload")
    public Response reload() {
        reloadRegistry();
        return Response.status(HttpStatusCode.CREATED_201).build();
    }
}
