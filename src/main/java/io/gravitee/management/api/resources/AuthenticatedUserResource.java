package io.gravitee.management.api.resources;

import io.gravitee.repository.model.User;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 *
 * @author David BRASSELY (brasseld at gmail.com)
 */
@Component
@Produces(MediaType.APPLICATION_JSON)
@Path("/user")
public class AuthenticatedUserResource {

    /**
     * Get the authenticated user.
     * @return The authenticated user.
     */
    @GET
    public User authenticatedUser() {
        return null;
    }

}
