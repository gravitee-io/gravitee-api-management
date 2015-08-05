package io.gravitee.management.api.resources;

import io.gravitee.repository.api.TeamRepository;
import io.gravitee.repository.model.Team;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
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
    private TeamRepository teamRepository;

    /**
     * List all public teams.
     * @return
     */
    @GET
    public Set<Team> listAll() {
        Set<Team> teams = teamRepository.findAll();

        if (teams == null) {
            teams = new HashSet<>();
        }

        return teams;
    }

    @Path("{teamName}")
    public TeamResource getTeamResource(@PathParam("teamName") String teamName) {
        TeamResource teamResource = resourceContext.getResource(TeamResource.class);
        teamResource.setTeamName(teamName);

        return teamResource;
    }
}
