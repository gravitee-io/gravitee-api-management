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

import io.gravitee.management.api.model.TeamEntity;
import io.gravitee.management.api.service.TeamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Optional;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
@Component
@Produces(MediaType.APPLICATION_JSON)
public class TeamResource {

    private String teamName;

    @Context
    private ResourceContext resourceContext;

    @Autowired
    private TeamService teamService;

    @GET
    public Response get() {
        Optional<TeamEntity> optTeam = teamService.findByName(teamName);

        if (optTeam.isPresent()) {
            return Response
                    .ok()
                    .entity(optTeam.get())
                    .build();
        }

        return Response.status(Response.Status.NOT_FOUND).build();
    }

    @Path("apis")
    public TeamApisResource getTeamApisResource() {
        TeamApisResource teamApisResource = resourceContext.getResource(TeamApisResource.class);
        teamApisResource.setTeamName(teamName);
        return teamApisResource;
    }

    @Path("applications")
    public TeamApplicationsResource getTeamApplicationsResource() {
        TeamApplicationsResource teamApplicationsResource = resourceContext.getResource(TeamApplicationsResource.class);
        teamApplicationsResource.setTeamName(teamName);
        return teamApplicationsResource;
    }

    @Path("members")
    public TeamApplicationsResource getTeamMembersResource() {
        TeamApplicationsResource teamApplicationsResource = resourceContext.getResource(TeamApplicationsResource.class);
        teamApplicationsResource.setTeamName(teamName);
        return teamApplicationsResource;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }
}
