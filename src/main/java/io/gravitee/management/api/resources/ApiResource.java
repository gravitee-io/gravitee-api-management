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
import io.gravitee.management.api.custom.LifecycleActionParam;
import io.gravitee.repository.api.ApiRepository;
import io.gravitee.repository.model.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.HttpHeaders.LOCATION;

/**
 * Defines the REST resources to manage {@code Api}.
 *
 * @author Azize Elamrani (azize dot elamrani at gmail dot com)
 * @author David BRASSELY (brasseld at gmail.com)
 */
@Component
@Produces(MediaType.APPLICATION_JSON)
public class ApiResource {

    @Autowired
    private ApiRepository apiRepository;

    private String apiName;

    @GET
    public Api get() {
        return apiRepository.findByName(apiName);
    }

    @POST
    public Response doLifecycleAction(@QueryParam("action") LifecycleActionParam action) {
        switch (action.getAction()) {
            case START:
                apiRepository.start(apiName);
                break;
            case STOP:
                apiRepository.stop(apiName);
                break;
            default:
                break;
        }

        return Response.status(HttpStatusCode.OK_200).build();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response update(final Api api) {
        Api updatedApi = apiRepository.update(api);
        if (updatedApi != null) {
            return Response.status(HttpStatusCode.OK_200).header(LOCATION, "/rest/apis/" +
                    api.getName()).entity(updatedApi).build();
        } else {
            return Response.status(HttpStatusCode.BAD_REQUEST_400).build();
        }
    }

    @DELETE
    public Response delete() {
        apiRepository.delete(apiName);
        return Response.status(HttpStatusCode.OK_200).build();
    }

    public void setApiName(String apiName) {
        this.apiName = apiName;
    }
}
