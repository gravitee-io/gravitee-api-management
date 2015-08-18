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

import io.gravitee.management.model.NewTeamEntity;
import io.gravitee.management.model.TeamEntity;
import io.gravitee.management.service.TeamService;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
@Path("/teams")
public class TeamsResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private TeamService teamService;

    /**
     * List all public teams.
     * @return
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Set<TeamEntity> listAll() {
        Set<TeamEntity> teams = teamService.findAll(true);

        if (teams == null) {
            teams = new HashSet<>();
        }

        return teams;
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(@Valid NewTeamEntity newTeamEntity) {
        TeamEntity team = teamService.create(newTeamEntity, getAuthenticatedUser());
        return Response
                .created(URI.create("/teams/" + team.getName()))
                .entity(team)
                .build();
    }

    @Path("{teamName}")
    public TeamResource getTeamResource() {
        return resourceContext.getResource(TeamResource.class);
    }
}
