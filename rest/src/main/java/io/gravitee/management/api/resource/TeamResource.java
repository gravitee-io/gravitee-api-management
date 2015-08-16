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

import io.gravitee.management.api.exceptions.TeamNotFoundException;
import io.gravitee.management.api.model.TeamEntity;
import io.gravitee.management.api.model.UpdateTeamEntity;
import io.gravitee.management.api.service.TeamService;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.Optional;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class TeamResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private TeamService teamService;

    @PathParam("teamName")
    private String teamName;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public TeamEntity get() {
        Optional<TeamEntity> optTeam = teamService.findByName(teamName);

        if (! optTeam.isPresent()) {
            throw new TeamNotFoundException(teamName);
        }

        return optTeam.get();
    }

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public TeamEntity update(@Valid UpdateTeamEntity updateTeamEntity) {
        return teamService.update(teamName, updateTeamEntity);
    }

    @Path("apis")
    public TeamApisResource getTeamApisResource() {
        return resourceContext.getResource(TeamApisResource.class);
    }

    @Path("applications")
    public TeamApplicationsResource getTeamApplicationsResource() {
        return resourceContext.getResource(TeamApplicationsResource.class);
    }

    @Path("members")
    public TeamMembershipsResource getTeamMembersResource() {
        return resourceContext.getResource(TeamMembershipsResource.class);
    }
}
