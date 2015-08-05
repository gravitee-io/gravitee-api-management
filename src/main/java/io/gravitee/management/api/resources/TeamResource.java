package io.gravitee.management.api.resources;

import io.gravitee.repository.api.TeamRepository;
import io.gravitee.repository.model.Team;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
@Component
@Produces(MediaType.APPLICATION_JSON)
public class TeamResource {

    private String teamName;

    @Autowired
    private TeamRepository teamRepository;

    @GET
    public Team get() {
        return teamRepository.findByName(teamName);
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }
}
