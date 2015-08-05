package io.gravitee.management.api.resources;

import io.gravitee.repository.model.Api;
import io.gravitee.repository.model.Team;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import java.util.Set;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class UserResource {

    private String username;

    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * List public APIs for the specified user.
     * @return Public APIs for the specified user.
     */
    @GET
    @Path("apis")
    public Set<Api> userApis() {
        return null;
    }

    /**
     * List public teams for the specified user.
     * @return Public teams for the specified user.
     */
    @GET
    @Path("teams")
    public Set<Team> userTeams() {
        return null;
    }
}
