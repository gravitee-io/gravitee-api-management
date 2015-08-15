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

import io.gravitee.repository.model.Application;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Set;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class TeamMembershipsResource {

    @PathParam("teamName")
    private String teamName;

    /**
     * List applications for the specified team.
     * @return Applications for the specified team.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Set<Application> getApplications() {
        return null;
    }

    @Path("{username}")
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    public Response addMember(@PathParam("username") String username, @QueryParam("role") String role) {
        return null;
    }

    @Path("{username}")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeMember(@PathParam("username") String username) {
        return null;
    }
}
