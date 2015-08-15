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
package io.gravitee.management.api.resource;

import io.gravitee.management.api.model.ApiEntity;
import io.gravitee.management.api.model.NewApiEntity;
import io.gravitee.management.api.service.ApiService;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Set;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class TeamApisResource {

    @Inject
    private ApiService apiService;

    @PathParam("teamName")
    private String teamName;

    /**
     * List APIs for the specified team.
     * @return APIs for the specified team.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Set<ApiEntity> getApis() {
        return apiService.findByTeam(teamName, true);
    }

    /**
     * Create a new API for the team.
     * @param api
     * @return
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createApi(NewApiEntity api) {
        ApiEntity createdApi = apiService.createForTeam(api, teamName);
        if (createdApi != null) {
            return Response
                    .created(URI.create("/apis/" + createdApi.getName()))
                    .entity(createdApi)
                    .build();
        }

        return Response.serverError().build();
    }
}
