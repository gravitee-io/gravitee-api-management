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

import io.gravitee.management.api.model.TeamCreation;
import io.gravitee.management.api.model.TeamEntity;
import io.gravitee.management.api.service.TeamService;
import io.gravitee.repository.api.TeamRepository;
import io.gravitee.repository.model.Team;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
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
@Component
@Produces(MediaType.APPLICATION_JSON)
@Path("/teams")
public class TeamsResource {

    @Context
    private ResourceContext resourceContext;

    @Autowired
    private TeamService teamService;

    /**
     * List all public teams.
     * @return
     */
    @GET
    public Set<TeamEntity> listAll() {
        Set<TeamEntity> teams = teamService.findAll(true);

        if (teams == null) {
            teams = new HashSet<>();
        }

        return teams;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(@NotNull TeamCreation teamCreation) {
        TeamEntity team = teamService.create(teamCreation);
        if (team != null) {
            return Response
                    .created(URI.create("/teams/" + team.getName()))
                    .entity(team)
                    .build();
        }

        return Response.serverError().build();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response update(@NotNull TeamCreation teamCreation) {
        TeamEntity team = teamService.update(teamCreation);
        if (team != null) {
            return Response
                    .ok()
                    .entity(team)
                    .build();
        }

        return Response.serverError().build();
    }

    @Path("{teamName}")
    public TeamResource getTeamResource(@PathParam("teamName") String teamName) {
        TeamResource teamResource = resourceContext.getResource(TeamResource.class);
        teamResource.setTeamName(teamName);

        return teamResource;
    }
}
