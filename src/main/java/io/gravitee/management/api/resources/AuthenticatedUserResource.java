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

import io.gravitee.repository.model.Api;
import io.gravitee.repository.model.Application;
import io.gravitee.repository.model.Team;
import io.gravitee.repository.model.User;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Set;

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

    /**
     * List teams that are accessible to the authenticated user.
     * @return
     */
    @GET
    @Path("teams")
    public Set<Team> listTeams() {
        return null;
    }

    /**
     * List applications that are accessible to the authenticated user.
     * @return
     */
    @GET
    @Path("applications")
    public Set<Application> listApplications() {
        return null;
    }

    /**
     * List APIs that are accessible to the authenticated user.
     * <p>
     * This includes APIs owned by the authenticated user and APIs that the authenticated user has access to through
     * a team membership.
     * </p>
     *
     * @return APIs that are accessible to the authenticated user.
     */
    @GET
    @Path("apis")
    public Set<Api> listApis() {
        return null;
    }

    /**
     * Create a new API for the authenticated user.
     * @param api
     * @return
     */
    @POST
    @Path("apis")
    public Api createApi(Api api) {
        return null;
    }

    /**
     * Create a new application for the authenticated user.
     * @param application
     * @return
     */
    @POST
    @Path("applications")
    public Application createApplication(Application application) {
        return null;
    }
}
